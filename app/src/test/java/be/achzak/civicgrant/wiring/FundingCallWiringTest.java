package be.achzak.civicgrant.wiring;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.fundingcall.application.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.core.fundingcall.application.port.in.OpenFundingCallUseCase;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@WiringTest
@DisplayName("Funding call wiring")
class FundingCallWiringTest {

    @Autowired
    private OpenFundingCallUseCase openFundingCall;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("stores an opened funding call with nothing committed yet")
    void storesAnOpenedFundingCallWithNothingCommittedYet() {
        // given
        Actor programmeManager = new Actor(new UserId("programme-manager-1"), Set.of(Role.PROGRAMME_MANAGER));
        Instant opensAt = Instant.now().plus(Duration.ofDays(14)).truncatedTo(ChronoUnit.SECONDS);
        OpenFundingCallCommand opening = new OpenFundingCallCommand(
                new FundingCallReference("GRN-WIRING"),
                new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                new SubmissionPeriod(opensAt, opensAt.plus(Duration.ofDays(90))),
                euros("80000.00"),
                euros("20000.00"),
                programmeManager);

        // when
        FundingCallId id = openFundingCall.open(opening);

        // then
        assertThat(jdbc.sql("""
                                SELECT reference, title_fr, opens_at, total_budget, committed_amount,
                                       max_amount_per_application
                                FROM civicgrant.funding_call WHERE id = :id
                                """)
                        .param("id", id.value())
                        .query((row, number) -> List.of(
                                row.getString("reference"),
                                row.getString("title_fr"),
                                row.getObject("opens_at", OffsetDateTime.class).toInstant(),
                                row.getBigDecimal("total_budget"),
                                row.getBigDecimal("committed_amount"),
                                row.getBigDecimal("max_amount_per_application")))
                        .single())
                .containsExactly(
                        "GRN-WIRING",
                        "Toitures vertes 2027",
                        opensAt,
                        new BigDecimal("80000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("20000.00"));
    }

    @Test
    @DisplayName("writes an audit entry when a funding call is opened")
    void writesAnAuditEntryWhenAFundingCallIsOpened() {
        // given
        Actor programmeManager = new Actor(new UserId("programme-manager-1"), Set.of(Role.PROGRAMME_MANAGER));
        Instant opensAt = Instant.now().plus(Duration.ofDays(14));
        OpenFundingCallCommand opening = new OpenFundingCallCommand(
                new FundingCallReference("GRN-AUDIT"),
                new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                new SubmissionPeriod(opensAt, opensAt.plus(Duration.ofDays(90))),
                euros("80000.00"),
                euros("20000.00"),
                programmeManager);

        // when
        FundingCallId id = openFundingCall.open(opening);

        // then
        assertThat(jdbc.sql("SELECT event_type, actor_oid FROM civicgrant.audit_event WHERE funding_call_id = :id")
                        .param("id", id.value())
                        .query((row, number) -> row.getString("event_type") + " by " + row.getString("actor_oid"))
                        .list())
                .containsExactly("FUNDING_CALL_OPENED by programme-manager-1");
    }
}
