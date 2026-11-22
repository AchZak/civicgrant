package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.event.GrantApplicationEvent;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
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

    private static ProjectProposal requesting(String requestedAmount) {
        return aProjectProposal()
                .withRequestedAmount(euros(requestedAmount))
                .withTotalProjectBudget(euros("20000.00"))
                .build();
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
