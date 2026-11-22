package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.OPENS_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationSubmitted;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Submitting a grant application")
class GrantApplicationSubmissionTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    private final FundingCall call = aFundingCall().build();

    private static ProjectProposal requesting(String requestedAmount) {
        return aProjectProposal()
                .withRequestedAmount(euros(requestedAmount))
                .withTotalProjectBudget(euros("20000.00"))
                .build();
    }

    @Test
    @DisplayName("moves the draft to submitted at the moment of submission")
    void movesTheDraftToSubmitted() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        application.submit(call, anApplicant(), NOW);

        // then
        assertThat(application.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.submittedAt()).contains(NOW);
    }

    @Test
    @DisplayName("records ApplicationSubmitted")
    void recordsApplicationSubmitted() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        application.submit(call, anApplicant(), NOW);

        // then
        assertThat(application.pullEvents())
                .containsExactly(new ApplicationSubmitted(
                        application.id(),
                        application.proposal().requestedAmount(),
                        anApplicant().id(),
                        NOW));
    }

    @Test
    @DisplayName("rejects a submission one second before the call opens")
    void rejectsSubmissionBeforeTheCallOpens() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();
        Instant justBeforeOpening = OPENS_AT.minusSeconds(1);

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), justBeforeOpening));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("SUBMISSION_BEFORE_OPENING_DATE");
    }

    @Test
    @DisplayName("accepts a submission at the moment the call opens")
    void acceptsSubmissionAtTheOpeningMoment() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        application.submit(call, anApplicant(), OPENS_AT);

        // then
        assertThat(application.status()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("accepts a submission one second before the call closes")
    void acceptsSubmissionJustBeforeClosing() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        application.submit(call, anApplicant(), CLOSES_AT.minusSeconds(1));

        // then
        assertThat(application.status()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("rejects a submission at the moment the call closes")
    void rejectsSubmissionAtTheClosingMoment() {
        // given
        GrantApplication application = aDraftApplication().forCall(call).build();

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), CLOSES_AT));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("SUBMISSION_AFTER_CLOSING_DATE");
    }

    @Test
    @DisplayName("rejects a draft without supporting documents")
    void rejectsDraftWithoutSupportingDocuments() {
        // given
        GrantApplication application =
                aDraftApplication().forCall(call).withoutDocuments().build();

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("SUPPORTING_DOCUMENT_REQUIRED");
    }

    @Test
    @DisplayName("rejects a second submission")
    void rejectsSecondSubmission() {
        // given
        GrantApplication application = aSubmittedApplication().forCall(call).build();

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), NOW));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
    }

    @Test
    @DisplayName("checks the call's cap again")
    void checksTheCapAgain() {
        // given
        GrantApplication application = aDraftApplication()
                .forCall(call)
                .withProposal(requesting("15000.01"))
                .build();

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("REQUESTED_AMOUNT_EXCEEDS_CAP");
    }

    @Test
    @DisplayName("refuses a funding call that isn't the application's own")
    void refusesFundingCallThatIsNotItsOwn() {
        // given
        GrantApplication application = aDraftApplication().build();

        // when
        Throwable thrown = catchThrowable(() -> application.submit(call, anApplicant(), NOW));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
