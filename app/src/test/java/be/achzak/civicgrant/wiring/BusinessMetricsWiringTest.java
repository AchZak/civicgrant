package be.achzak.civicgrant.wiring;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.wiring.WiringFixtures.DECISION_MAKER;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

@WiringTest
@DisplayName("Business metrics wiring")
class BusinessMetricsWiringTest {

    // The monitoring workbook queries these names, so the test pins them.
    private static final String APPLICATIONS_METER = "civicgrant.applications";
    private static final String GRANTED_AMOUNTS_METER = "civicgrant.grants.amount";

    @Autowired
    private ApproveApplicationUseCase approveApplication;

    @Autowired
    private MeterRegistry meters;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private WiringFixtures fixtures;

    @Test
    @DisplayName("counts a committed approval and its granted amount in the business metrics")
    void countsACommittedApprovalAndItsGrantedAmount() {
        // given
        ApplicationId id = fixtures.anAssessedApplication("NBH-METRICS", "0433000476");
        double approvalsBefore = approvalCount();
        long amountsBefore = grantedAmounts().count();
        double totalBefore = grantedAmounts().totalAmount();

        // when
        approveApplication.approve(
                new ApproveApplicationCommand(id, euros("6000.00"), "Het project versterkt de buurt.", DECISION_MAKER));

        // then
        assertThat(approvalCount()).isEqualTo(approvalsBefore + 1);
        assertThat(grantedAmounts().count()).isEqualTo(amountsBefore + 1);
        assertThat(grantedAmounts().totalAmount()).isEqualTo(totalBefore + 6000.00);
    }

    @Test
    @DisplayName("counts nothing for an approval that is rolled back")
    void countsNothingForAnApprovalThatIsRolledBack() {
        // given
        ApplicationId id = fixtures.anAssessedApplication("NBH-ROLLBACK", "0433000575");
        double approvalsBefore = approvalCount();

        // when
        transactions.executeWithoutResult(transaction -> {
            approveApplication.approve(new ApproveApplicationCommand(
                    id, euros("6000.00"), "Het project versterkt de buurt.", DECISION_MAKER));
            transaction.setRollbackOnly();
        });

        // then
        assertThat(jdbc.sql("SELECT status FROM civicgrant.grant_application WHERE id = :id")
                        .param("id", id.value())
                        .query(String.class)
                        .single())
                .isEqualTo("ASSESSED");
        assertThat(approvalCount()).isEqualTo(approvalsBefore);
    }

    private double approvalCount() {
        return meters.get(APPLICATIONS_METER).tag("event", "approved").counter().count();
    }

    private DistributionSummary grantedAmounts() {
        return meters.get(GRANTED_AMOUNTS_METER).summary();
    }
}
