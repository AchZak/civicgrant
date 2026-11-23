package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.aDecisionMaker;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.actor;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.anAssessor;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anAssessedApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.common.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.event.GrantCommitted;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.Decision;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationApproved;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Approving a grant application")
class ApproveApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T09:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Money GRANTED = euros("7500.00");
    private static final String MOTIVATION = "Het project versterkt de buurt en de begroting is onderbouwd.";

    private final FundingCall fundingCall =
            aFundingCall().withCommitted(euros("12500.00")).build();
    private final FundingCall nearlySpentCall = aFundingCall()
            .withReference("NBH-2026-B")
            .withCommitted(euros("45000.00"))
            .build();
    private final GrantApplication assessed =
            anAssessedApplication().forCall(fundingCall).build();
    private final GrantApplication assessedInNearlySpentCall =
            anAssessedApplication().forCall(nearlySpentCall).build();
    private final GrantApplication submitted =
            aSubmittedApplication().forCall(fundingCall).build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(assessed, assessedInNearlySpentCall, submitted);
    private final InMemoryFundingCallRepository fundingCalls =
            new InMemoryFundingCallRepository(fundingCall, nearlySpentCall);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final ApproveApplicationService service =
            new ApproveApplicationService(applications, fundingCalls, events, CLOCK);

    @Test
    @DisplayName("approves the granted amount by the decision maker at the current moment")
    void approvesTheGrantedAmountByTheDecisionMakerAtTheCurrentMoment() {
        // when
        service.approve(approvalOf(assessed.id(), aDecisionMaker()));

        // then
        GrantApplication stored = applications.findById(assessed.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(stored.decision())
                .contains(new Decision.Approval(GRANTED, aDecisionMaker().id(), MOTIVATION, NOW));
    }

    @Test
    @DisplayName("commits the granted amount to the budget of the application's call")
    void commitsTheGrantedAmountToTheBudgetOfTheApplicationsCall() {
        // when
        service.approve(approvalOf(assessed.id(), aDecisionMaker()));

        // then
        FundingCall stored = fundingCalls.findById(fundingCall.id()).orElseThrow();
        assertThat(stored.budget().committed()).isEqualTo(euros("20000.00"));
    }

    @Test
    @DisplayName("publishes the approval, then the committed grant")
    void publishesTheApprovalThenTheCommittedGrant() {
        // when
        service.approve(approvalOf(assessed.id(), aDecisionMaker()));

        // then
        assertThat(events.published())
                .containsExactly(
                        new ApplicationApproved(
                                assessed.id(), GRANTED, aDecisionMaker().id(), NOW),
                        new GrantCommitted(
                                fundingCall.id(), GRANTED, aDecisionMaker().id(), NOW));
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.approve(approvalOf(ApplicationId.generate(), aDecisionMaker())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses a grant above the call's remaining budget and publishes nothing")
    void refusesAGrantAboveTheCallsRemainingBudget() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.approve(approvalOf(assessedInNearlySpentCall.id(), aDecisionMaker())));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANT_EXCEEDS_REMAINING_BUDGET");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("leaves the budget untouched when the application can't be approved")
    void leavesTheBudgetUntouchedWhenTheApplicationCantBeApproved() {
        // when
        Throwable thrown = catchThrowable(() -> service.approve(approvalOf(submitted.id(), aDecisionMaker())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        assertThat(fundingCall.budget().committed()).isEqualTo(euros("12500.00"));
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("refuses the assessor of the application, even with the decision maker role")
    void refusesTheAssessorOfTheApplication() {
        // given
        Actor assessorWhoAlsoDecides = actor(anAssessor().id().value(), Role.ASSESSOR, Role.DECISION_MAKER);

        // when
        Throwable thrown = catchThrowable(() -> service.approve(approvalOf(assessed.id(), assessorWhoAlsoDecides)));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("SEPARATION_OF_DUTIES");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "DECISION_MAKER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("refuses an actor without the decision maker role")
    void refusesAnActorWithoutTheDecisionMakerRole(Role role) {
        // when
        Throwable thrown = catchThrowable(() -> service.approve(approvalOf(assessed.id(), actor("staff-1", role))));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    private ApproveApplicationCommand approvalOf(ApplicationId applicationId, Actor actor) {
        return new ApproveApplicationCommand(applicationId, GRANTED, MOTIVATION, actor);
    }
}
