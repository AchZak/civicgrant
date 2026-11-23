package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.aDecisionMaker;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anAssessedApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.common.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Approving a grant application")
class ApproveApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T09:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FundingCall fundingCall =
            aFundingCall().withCommitted(euros("12500.00")).build();
    private final FundingCall nearlySpentCall = aFundingCall()
            .withReference("NBH-2026-B")
            .withCommitted(euros("45000.00"))
            .build();
    private final GrantApplication assessedInNearlySpentCall =
            anAssessedApplication().forCall(nearlySpentCall).build();
    private final GrantApplication submitted =
            aSubmittedApplication().forCall(fundingCall).build();
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final ApproveApplicationService service = new ApproveApplicationService(
            new InMemoryGrantApplicationRepository(assessedInNearlySpentCall, submitted),
            new InMemoryFundingCallRepository(fundingCall, nearlySpentCall),
            events,
            CLOCK);

    @Test
    @DisplayName("refuses a grant above the call's remaining budget and publishes nothing")
    void refusesAGrantAboveTheCallsRemainingBudget() {
        // when
        Throwable thrown = catchThrowable(() -> service.approve(approvalOf(assessedInNearlySpentCall.id())));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANT_EXCEEDS_REMAINING_BUDGET");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("leaves the budget untouched when the application can't be approved")
    void leavesTheBudgetUntouchedWhenTheApplicationCantBeApproved() {
        // when
        Throwable thrown = catchThrowable(() -> service.approve(approvalOf(submitted.id())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        assertThat(fundingCall.budget().committed()).isEqualTo(euros("12500.00"));
        assertThat(events.published()).isEmpty();
    }

    private static ApproveApplicationCommand approvalOf(ApplicationId applicationId) {
        return new ApproveApplicationCommand(
                applicationId,
                euros("7500.00"),
                "Het project versterkt de buurt en de begroting is onderbouwd.",
                aDecisionMaker());
    }
}
