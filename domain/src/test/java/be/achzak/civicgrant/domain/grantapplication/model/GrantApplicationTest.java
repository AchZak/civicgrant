package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.OPENS_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.ASSESSED_AT;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aRejectedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anApprovedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Grant application")
class GrantApplicationTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final ApplicationReference REFERENCE = new ApplicationReference("CG-2026-000042");

    private final FundingCall call = aFundingCall().build();

    private GrantApplication create(Organisation organisation, ProjectProposal proposal, Instant now) {
        return GrantApplication.create(
                REFERENCE, call, organisation, proposal, EligibilityPolicy.nonProfits(), anApplicant(), now);
    }

    private static ProjectProposal requesting(String requestedAmount) {
        return aProjectProposal()
                .withRequestedAmount(euros(requestedAmount))
                .withTotalProjectBudget(euros("20000.00"))
                .build();
    }

    @Nested
    @DisplayName("Creating an application")
    class Create {

        @Test
        @DisplayName("starts as a draft of its applicant in the given call")
        void startsAsDraftOfItsApplicant() {
            // when
            GrantApplication application =
                    create(anOrganisation().build(), aProjectProposal().build(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(application.applicant()).isEqualTo(anApplicant().id());
            assertThat(application.fundingCallId()).isEqualTo(call.id());
            assertThat(application.submittedAt()).isEmpty();
            assertThat(application.documents()).isEmpty();
        }

        @Test
        @DisplayName("records ApplicationCreated")
        void recordsApplicationCreated() {
            // given
            Organisation organisation = anOrganisation().build();

            // when
            GrantApplication application =
                    create(organisation, aProjectProposal().build(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new ApplicationCreated(
                            application.id(),
                            call.id(),
                            organisation.id(),
                            anApplicant().id(),
                            NOW));
        }

        @Test
        @DisplayName("may be prepared before the call opens")
        void mayBePreparedBeforeTheCallOpens() {
            // given
            Instant aWeekBeforeOpening = OPENS_AT.minus(Duration.ofDays(7));

            // when
            GrantApplication application =
                    create(anOrganisation().build(), aProjectProposal().build(), aWeekBeforeOpening);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.DRAFT);
        }

        @Test
        @DisplayName("rejects a new draft once the call has closed")
        void rejectsNewDraftOnceTheCallHasClosed() {
            // when
            Throwable thrown = catchThrowable(
                    () -> create(anOrganisation().build(), aProjectProposal().build(), CLOSES_AT));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_CLOSED");
        }

        @Test
        @DisplayName("rejects an organisation whose legal form isn't eligible")
        void rejectsOrganisationWhoseLegalFormIsNotEligible() {
            // given
            Organisation company =
                    anOrganisation().withLegalForm(LegalForm.COMPANY).build();

            // when
            Throwable thrown =
                    catchThrowable(() -> create(company, aProjectProposal().build(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("LEGAL_FORM_NOT_ELIGIBLE");
        }

        @Test
        @DisplayName("accepts a requested amount of exactly the call's cap")
        void acceptsRequestedAmountOfExactlyTheCap() {
            // given
            ProjectProposal atTheCap = requesting("15000.00");

            // when
            GrantApplication application = create(anOrganisation().build(), atTheCap, NOW);

            // then
            assertThat(application.proposal()).isEqualTo(atTheCap);
        }

        @Test
        @DisplayName("rejects a requested amount one cent above the call's cap")
        void rejectsRequestedAmountOneCentAboveTheCap() {
            // given
            ProjectProposal aboveTheCap = requesting("15000.01");

            // when
            Throwable thrown = catchThrowable(() -> create(anOrganisation().build(), aboveTheCap, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("REQUESTED_AMOUNT_EXCEEDS_CAP");
        }
    }

    @Nested
    @DisplayName("Editing a draft")
    class Edit {

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

    @Nested
    @DisplayName("Supporting documents")
    class SupportingDocuments {

        private static final String PDF = "application/pdf";

        @Test
        @DisplayName("attaches a document to a draft")
        void attachesDocumentToDraft() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            SupportingDocument document =
                    application.attachDocument("begroting.pdf", PDF, 245_760, call, anApplicant(), NOW);

            // then
            assertThat(application.documents()).containsExactly(document);
            assertThat(document.fileName()).isEqualTo("begroting.pdf");
            assertThat(document.uploadedBy()).isEqualTo(anApplicant().id());
            assertThat(document.uploadedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("records DocumentAttached")
        void recordsDocumentAttached() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            SupportingDocument document =
                    application.attachDocument("begroting.pdf", PDF, 245_760, call, anApplicant(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new DocumentAttached(
                            application.id(), document.id(), anApplicant().id(), NOW));
        }

        @Test
        @DisplayName("leaves the draft unchanged when the document is refused")
        void leavesTheDraftUnchangedWhenTheDocumentIsRefused() {
            // given
            GrantApplication application =
                    aDraftApplication().forCall(call).withoutDocuments().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("foto.png", "image/png", 1024, call, anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
            assertThat(application.documents()).isEmpty();
            assertThat(application.pullEvents()).isEmpty();
        }

        @Test
        @DisplayName("rejects a document for a submitted application")
        void rejectsDocumentForSubmittedApplication() {
            // given
            GrantApplication application = aSubmittedApplication().forCall(call).build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("begroting.pdf", PDF, 245_760, call, anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
        }

        @Test
        @DisplayName("rejects a new document once the call has closed")
        void rejectsNewDocumentOnceTheCallHasClosed() {
            // given
            GrantApplication application = aDraftApplication().forCall(call).build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.attachDocument("begroting.pdf", PDF, 245_760, call, anApplicant(), CLOSES_AT));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_CLOSED");
        }

        @Test
        @DisplayName("removes a document from a draft")
        void removesDocumentFromDraft() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            application.removeDocument(document.id(), anApplicant(), NOW);

            // then
            assertThat(application.documents()).isEmpty();
            assertThat(application.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("records DocumentRemoved")
        void recordsDocumentRemoved() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aDraftApplication().withDocuments(document).build();

            // when
            application.removeDocument(document.id(), anApplicant(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new DocumentRemoved(
                            application.id(), document.id(), anApplicant().id(), NOW));
        }

        @Test
        @DisplayName("answers not found for a document the draft doesn't have")
        void answersNotFoundForUnknownDocument() {
            // given
            GrantApplication application = aDraftApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.removeDocument(DocumentId.generate(), anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("DOCUMENT_NOT_FOUND");
        }

        @Test
        @DisplayName("keeps the documents of a submitted application")
        void keepsTheDocumentsOfSubmittedApplication() {
            // given
            SupportingDocument document = aSupportingDocument();
            GrantApplication application =
                    aSubmittedApplication().withDocuments(document).build();

            // when
            Throwable thrown = catchThrowable(() -> application.removeDocument(document.id(), anApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
            assertThat(application.documents()).containsExactly(document);
        }
    }

    @Nested
    @DisplayName("Submitting a draft")
    class Submit {

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

    @Nested
    @DisplayName("Recording the assessment")
    class Assess {

        private static final String COMMENT = "Sterk buurtproject met een realistische begroting.";

        private static Recommendation proposing(String amount) {
            return new Recommendation.Approve(euros(amount));
        }

        @Test
        @DisplayName("moves the submitted application to assessed")
        void movesTheSubmittedApplicationToAssessed() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            application.recordAssessment(true, proposing("8000.00"), COMMENT, anAssessor(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.ASSESSED);
            assertThat(application.assessment())
                    .contains(new Assessment(anAssessor().id(), true, proposing("8000.00"), COMMENT, NOW));
        }

        @Test
        @DisplayName("records AssessmentRecorded")
        void recordsAssessmentRecorded() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            application.recordAssessment(true, proposing("8000.00"), COMMENT, anAssessor(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new AssessmentRecorded(
                            application.id(),
                            true,
                            proposing("8000.00"),
                            anAssessor().id(),
                            NOW));
        }

        @Test
        @DisplayName("accepts a rejection recommendation for an ineligible application")
        void acceptsRejectionRecommendationForIneligibleApplication() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            application.recordAssessment(false, new Recommendation.Reject(), COMMENT, anAssessor(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.ASSESSED);
        }

        @Test
        @DisplayName("rejects recommending an ineligible application for approval")
        void rejectsRecommendingIneligibleApplicationForApproval() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(false, proposing("8000.00"), COMMENT, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown)
                    .isBusinessRuleViolation()
                    .hasCode("APPROVAL_RECOMMENDATION_REQUIRES_ELIGIBILITY");
        }

        @Test
        @DisplayName("rejects proposing one cent more than the requested amount")
        void rejectsProposingOneCentMoreThanRequested() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(true, proposing("8000.01"), COMMENT, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROPOSED_AMOUNT_EXCEEDS_REQUESTED");
        }

        @Test
        @DisplayName("rejects proposing zero")
        void rejectsProposingZero() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(true, proposing("0.00"), COMMENT, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROPOSED_AMOUNT_NOT_POSITIVE");
        }

        @Test
        @DisplayName("strips whitespace around the comment")
        void stripsWhitespaceAroundTheComment() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            application.recordAssessment(false, new Recommendation.Reject(), "  " + COMMENT + "\n", anAssessor(), NOW);

            // then
            assertThat(application.assessment()).map(Assessment::comment).contains(COMMENT);
        }

        @Test
        @DisplayName("rejects a blank comment")
        void rejectsBlankComment() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(false, new Recommendation.Reject(), " ", anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("ASSESSMENT_COMMENT_REQUIRED");
        }

        @Test
        @DisplayName("rejects a comment longer than 4,000 characters")
        void rejectsCommentLongerThan4000Characters() {
            // given
            GrantApplication application = aSubmittedApplication().build();
            String tooLong = "a".repeat(4_001);

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(false, new Recommendation.Reject(), tooLong, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("ASSESSMENT_COMMENT_TOO_LONG");
        }

        @Test
        @DisplayName("answers not found for a draft, which staff can't see")
        void answersNotFoundForDraft() {
            // given
            GrantApplication application = aDraftApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(false, new Recommendation.Reject(), COMMENT, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("rejects a second assessment")
        void rejectsSecondAssessment() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.recordAssessment(false, new Recommendation.Reject(), COMMENT, anAssessor(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_SUBMITTED");
        }

        @Test
        @DisplayName("forbids the applicant to assess their own application")
        void forbidsTheApplicantToAssessTheirOwnApplication() {
            // given
            GrantApplication application = aSubmittedApplication().build();
            Actor applicantWhoAlsoAssesses = actor("applicant-1", Role.APPLICANT, Role.ASSESSOR);

            // when
            Throwable thrown = catchThrowable(() -> application.recordAssessment(
                    false, new Recommendation.Reject(), COMMENT, applicantWhoAlsoAssesses, NOW));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("CONFLICT_OF_INTEREST");
        }
    }

    @Nested
    @DisplayName("Approving")
    class Approving {

        private static final String MOTIVATION = "Het project versterkt de buurt en de begroting is onderbouwd.";

        @Test
        @DisplayName("moves the assessed application to approved, with its decision")
        void movesTheAssessedApplicationToApproved() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(application.decision())
                    .contains(new Decision.Approval(
                            euros("8000.00"), aDecisionMaker().id(), MOTIVATION, NOW));
        }

        @Test
        @DisplayName("records ApplicationApproved")
        void recordsApplicationApproved() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new ApplicationApproved(
                            application.id(), euros("8000.00"), aDecisionMaker().id(), NOW));
        }

        @Test
        @DisplayName("may grant less than the requested amount")
        void mayGrantLessThanTheRequestedAmount() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            application.approve(euros("5000.00"), MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.decision())
                    .contains(new Decision.Approval(
                            euros("5000.00"), aDecisionMaker().id(), MOTIVATION, NOW));
        }

        @Test
        @DisplayName("rejects granting one cent more than the requested amount")
        void rejectsGrantingOneCentMoreThanRequested() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("8000.01"), MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANTED_AMOUNT_EXCEEDS_REQUESTED");
        }

        @Test
        @DisplayName("rejects granting zero")
        void rejectsGrantingZero() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("0.00"), MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANTED_AMOUNT_NOT_POSITIVE");
        }

        @Test
        @DisplayName("may approve against a rejection recommendation")
        void mayApproveAgainstRejectionRecommendation() {
            // given
            GrantApplication application = anAssessedApplication()
                    .withAssessment(new Assessment(
                            anAssessor().id(), true, new Recommendation.Reject(), "Twijfelachtig plan.", ASSESSED_AT))
                    .build();

            // when
            application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("rejects approving an application assessed as ineligible")
        void rejectsApprovingApplicationAssessedAsIneligible() {
            // given
            GrantApplication application = anAssessedApplication()
                    .withAssessment(new Assessment(
                            anAssessor().id(), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT))
                    .build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown)
                    .isBusinessRuleViolation()
                    .hasCode("INELIGIBLE_APPLICATION_CANNOT_BE_APPROVED");
        }

        @Test
        @DisplayName("rejects a blank motivation")
        void rejectsBlankMotivation() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown = catchThrowable(() -> application.approve(euros("8000.00"), "  ", aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_REQUIRED");
        }

        @Test
        @DisplayName("rejects a motivation longer than 4,000 characters")
        void rejectsMotivationLongerThan4000Characters() {
            // given
            GrantApplication application = anAssessedApplication().build();
            String tooLong = "a".repeat(4_001);

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("8000.00"), tooLong, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_TOO_LONG");
        }

        @Test
        @DisplayName("forbids the assessor to decide")
        void forbidsTheAssessorToDecide() {
            // given
            GrantApplication application = anAssessedApplication().build();
            Actor assessorWhoAlsoDecides = actor("assessor-1", Role.ASSESSOR, Role.DECISION_MAKER);

            // when
            Throwable thrown = catchThrowable(
                    () -> application.approve(euros("8000.00"), MOTIVATION, assessorWhoAlsoDecides, NOW));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("SEPARATION_OF_DUTIES");
        }

        @Test
        @DisplayName("forbids the applicant to decide")
        void forbidsTheApplicantToDecide() {
            // given
            GrantApplication application = anAssessedApplication().build();
            Actor applicantWhoAlsoDecides = actor("applicant-1", Role.APPLICANT, Role.DECISION_MAKER);

            // when
            Throwable thrown = catchThrowable(
                    () -> application.approve(euros("8000.00"), MOTIVATION, applicantWhoAlsoDecides, NOW));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("CONFLICT_OF_INTEREST");
        }

        @Test
        @DisplayName("rejects a decision before the assessment")
        void rejectsDecisionBeforeTheAssessment() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        }

        @Test
        @DisplayName("rejects a second decision")
        void rejectsSecondDecision() {
            // given
            GrantApplication application = anApprovedApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.approve(euros("8000.00"), MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        }
    }

    @Nested
    @DisplayName("Rejecting")
    class Rejecting {

        private static final String MOTIVATION = "Het projectplan is onvoldoende uitgewerkt.";

        @Test
        @DisplayName("moves the assessed application to rejected, with its decision")
        void movesTheAssessedApplicationToRejected() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            application.reject(RejectionGround.INSUFFICIENT_QUALITY, MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(application.decision())
                    .contains(new Decision.Rejection(
                            RejectionGround.INSUFFICIENT_QUALITY,
                            aDecisionMaker().id(),
                            MOTIVATION,
                            NOW));
        }

        @Test
        @DisplayName("records ApplicationRejected")
        void recordsApplicationRejected() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            application.reject(RejectionGround.INCOMPLETE, MOTIVATION, aDecisionMaker(), NOW);

            // then
            assertThat(application.pullEvents())
                    .containsExactly(new ApplicationRejected(
                            application.id(),
                            RejectionGround.INCOMPLETE,
                            aDecisionMaker().id(),
                            NOW));
        }

        @Test
        @DisplayName("rejects a blank motivation")
        void rejectsBlankMotivation() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown =
                    catchThrowable(() -> application.reject(RejectionGround.INCOMPLETE, "", aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_REQUIRED");
        }

        @Test
        @DisplayName("forbids the assessor to decide")
        void forbidsTheAssessorToDecide() {
            // given
            GrantApplication application = anAssessedApplication().build();
            Actor assessorWhoAlsoDecides = actor("assessor-1", Role.ASSESSOR, Role.DECISION_MAKER);

            // when
            Throwable thrown = catchThrowable(
                    () -> application.reject(RejectionGround.INCOMPLETE, MOTIVATION, assessorWhoAlsoDecides, NOW));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("SEPARATION_OF_DUTIES");
        }

        @Test
        @DisplayName("rejects a second decision")
        void rejectsSecondDecision() {
            // given
            GrantApplication application = aRejectedApplication().build();

            // when
            Throwable thrown = catchThrowable(
                    () -> application.reject(RejectionGround.INCOMPLETE, MOTIVATION, aDecisionMaker(), NOW));

            // then
            assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        }
    }

    @Nested
    @DisplayName("Visibility")
    class Visibility {

        @Test
        @DisplayName("shows a draft to its applicant")
        void showsDraftToItsApplicant() {
            // given
            GrantApplication application = aDraftApplication().build();

            // when
            boolean visible = application.isVisibleTo(anApplicant());

            // then
            assertThat(visible).isTrue();
        }

        @Test
        @DisplayName("hides a draft from staff")
        void hidesDraftFromStaff() {
            // given
            GrantApplication application = aDraftApplication().build();

            // when
            boolean visibleToAssessor = application.isVisibleTo(anAssessor());
            boolean visibleToDecisionMaker = application.isVisibleTo(aDecisionMaker());

            // then
            assertThat(visibleToAssessor).isFalse();
            assertThat(visibleToDecisionMaker).isFalse();
        }

        @Test
        @DisplayName("shows a submitted application to staff")
        void showsSubmittedApplicationToStaff() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            boolean visibleToAssessor = application.isVisibleTo(anAssessor());
            boolean visibleToDecisionMaker = application.isVisibleTo(aDecisionMaker());

            // then
            assertThat(visibleToAssessor).isTrue();
            assertThat(visibleToDecisionMaker).isTrue();
        }

        @Test
        @DisplayName("hides an application from other applicants")
        void hidesApplicationFromOtherApplicants() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            boolean visible = application.isVisibleTo(anotherApplicant());

            // then
            assertThat(visible).isFalse();
        }
    }

    @Nested
    @DisplayName("Acting on someone else's application")
    class SomeoneElse {

        @Test
        @DisplayName("answers not found to another applicant, so ids can't be probed")
        void answersNotFoundToAnotherApplicant() {
            // given
            GrantApplication application = aDraftApplication().forCall(call).build();

            // when
            Throwable thrown = catchThrowable(() -> application.submit(call, anotherApplicant(), NOW));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("forbids a staff member to change an application they can see")
        void forbidsStaffMemberToChangeApplicationTheyCanSee() {
            // given
            GrantApplication application = aSubmittedApplication().forCall(call).build();
            Actor assessorWhoAlsoApplies = actor("assessor-2", Role.APPLICANT, Role.ASSESSOR);

            // when
            Throwable thrown =
                    catchThrowable(() -> application.edit(requesting("9000.00"), call, assessorWhoAlsoApplies, NOW));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("NOT_THE_APPLICANT");
        }
    }

    @Nested
    @DisplayName("Restoring")
    class Restore {

        private GrantApplicationSnapshot snapshot(
                ApplicationStatus status, Instant submittedAt, Assessment assessment, Decision decision) {
            return new GrantApplicationSnapshot(
                    ApplicationId.generate(),
                    REFERENCE,
                    FundingCallId.generate(),
                    OrganisationId.generate(),
                    anApplicant().id(),
                    aProjectProposal().build(),
                    List.of(),
                    status,
                    submittedAt,
                    assessment,
                    decision,
                    NOW,
                    NOW,
                    0);
        }

        @Test
        @DisplayName("records no events")
        void recordsNoEvents() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            List<GrantApplicationEvent> events = application.pullEvents();

            // then
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("refuses a stored draft with a submission time")
        void refusesStoredDraftWithSubmissionTime() {
            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.DRAFT, NOW, null, null));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a stored submitted application without a submission time")
        void refusesStoredSubmittedApplicationWithoutSubmissionTime() {
            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.SUBMITTED, null, null, null));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a stored assessed application without an assessment")
        void refusesStoredAssessedApplicationWithoutAssessment() {
            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.ASSESSED, NOW, null, null));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a stored submitted application with an assessment")
        void refusesStoredSubmittedApplicationWithAssessment() {
            // given
            Assessment assessment =
                    new Assessment(anAssessor().id(), false, new Recommendation.Reject(), "Onvolledig.", NOW);

            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.SUBMITTED, NOW, assessment, null));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a stored assessed application with a decision")
        void refusesStoredAssessedApplicationWithDecision() {
            // given
            Assessment assessment =
                    new Assessment(anAssessor().id(), false, new Recommendation.Reject(), "Onvolledig.", NOW);
            Decision rejection = new Decision.Rejection(
                    RejectionGround.INCOMPLETE, aDecisionMaker().id(), "Onvolledig.", NOW);

            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.ASSESSED, NOW, assessment, rejection));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a stored approved application with a rejection")
        void refusesStoredApprovedApplicationWithRejection() {
            // given
            Assessment assessment =
                    new Assessment(anAssessor().id(), false, new Recommendation.Reject(), "Onvolledig.", NOW);
            Decision rejection = new Decision.Rejection(
                    RejectionGround.INCOMPLETE, aDecisionMaker().id(), "Onvolledig.", NOW);

            // when
            Throwable thrown = catchThrowable(() -> snapshot(ApplicationStatus.APPROVED, NOW, assessment, rejection));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
