package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.ASSESSED_AT;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aRejectedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anApprovedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.aDecisionMaker;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anAssessor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationApproved;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationRejected;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Decision on a grant application")
class GrantApplicationDecisionTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

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
}
