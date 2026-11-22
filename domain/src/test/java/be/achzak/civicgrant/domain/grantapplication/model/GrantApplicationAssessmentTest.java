package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.ASSESSED_AT;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anApprovedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.grantapplication.model.event.AssessmentRecorded;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Assessment of a grant application")
class GrantApplicationAssessmentTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

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
    @DisplayName("Reading the assessment")
    class ReadAssessment {

        @Test
        @DisplayName("gives staff the assessment of an assessed application")
        void givesStaffTheAssessmentOfAnAssessedApplication() {
            // given
            Assessment assessment =
                    new Assessment(anAssessor().id(), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT);
            GrantApplication application =
                    anAssessedApplication().withAssessment(assessment).build();

            // when
            Assessment read = application.assessmentFor(aDecisionMaker());

            // then
            assertThat(read).isEqualTo(assessment);
        }

        @Test
        @DisplayName("keeps giving the assessment once the application is decided")
        void keepsGivingTheAssessmentOnceTheApplicationIsDecided() {
            // given
            GrantApplication application = anApprovedApplication().build();

            // when
            Assessment read = application.assessmentFor(anAssessor());

            // then
            assertThat(application.assessment()).contains(read);
        }

        @Test
        @DisplayName("hides the assessment from another applicant, as if the application didn't exist")
        void hidesTheAssessmentFromAnotherApplicant() {
            // given
            GrantApplication application = anAssessedApplication().build();

            // when
            Throwable thrown = catchThrowable(() -> application.assessmentFor(anotherApplicant()));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("forbids the applicant to read the assessment of their own application, even with a staff role")
        void forbidsTheApplicantToReadTheAssessmentOfTheirOwnApplication() {
            // given
            GrantApplication application = anAssessedApplication().build();
            Actor applicantWhoAlsoDecides = actor("applicant-1", Role.APPLICANT, Role.DECISION_MAKER);

            // when
            Throwable thrown = catchThrowable(() -> application.assessmentFor(applicantWhoAlsoDecides));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("CONFLICT_OF_INTEREST");
        }

        @Test
        @DisplayName("answers not found until the application is assessed")
        void answersNotFoundUntilTheApplicationIsAssessed() {
            // given
            GrantApplication application = aSubmittedApplication().build();

            // when
            Throwable thrown = catchThrowable(() -> application.assessmentFor(anAssessor()));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("ASSESSMENT_NOT_FOUND");
        }
    }
}
