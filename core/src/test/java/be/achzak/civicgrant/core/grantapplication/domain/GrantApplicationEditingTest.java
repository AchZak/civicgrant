package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationEdited;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Editing a draft grant application")
class GrantApplicationEditingTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    private final FundingCall call = aFundingCall().build();

    private static ProjectProposal requesting(String requestedAmount) {
        return aProjectProposal()
                .withRequestedAmount(euros(requestedAmount))
                .withTotalProjectBudget(euros("20000.00"))
                .build();
    }

    @Test
    @DisplayName("replaces the proposal")
    void replacesTheProposal() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();
        ProjectProposal newProposal = requesting("9000.00");

        // when
        application.edit(newProposal, call, anApplicant(), NOW);

        // then
        assertThat(application.proposal()).isEqualTo(newProposal);
        assertThat(application.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("records ApplicationEdited")
    void recordsApplicationEdited() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        application.edit(requesting("9000.00"), call, anApplicant(), NOW);

        // then
        assertThat(application.pullEvents())
                .containsExactly(new ApplicationEdited(
                        application.id(), euros("9000.00"), anApplicant().id(), NOW));
    }

    @Test
    @DisplayName("rejects a requested amount above the call's cap")
    void rejectsRequestedAmountAboveTheCap() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        Throwable thrown = catchThrowable(() -> application.edit(requesting("15000.01"), call, anApplicant(), NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("REQUESTED_AMOUNT_EXCEEDS_CAP");
    }

    @Test
    @DisplayName("rejects changes once the call has closed")
    void rejectsChangesOnceTheCallHasClosed() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        Throwable thrown =
                catchThrowable(() -> application.edit(requesting("9000.00"), call, anApplicant(), CLOSES_AT));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_CLOSED");
    }

    @Test
    @DisplayName("rejects changes to a submitted application")
    void rejectsChangesToSubmittedApplication() {
        // given
        GrantApplication application = aSubmittedApplication().forCall(call).build();

        // when
        Throwable thrown = catchThrowable(() -> application.edit(requesting("9000.00"), call, anApplicant(), NOW));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
    }
}
