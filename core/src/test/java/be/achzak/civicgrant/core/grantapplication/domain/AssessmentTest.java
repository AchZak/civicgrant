package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anAssessor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Recording an assessment")
class AssessmentTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Money REQUESTED = euros("8000.00");
    private static final String COMMENT = "Sterk buurtproject met een realistische begroting.";

    private static Recommendation proposing(String amount) {
        return new Recommendation.Approve(euros(amount));
    }

    @Test
    @DisplayName("accepts proposing exactly the requested amount for an eligible application")
    void acceptsProposingExactlyTheRequestedAmount() {
        // when
        Assessment assessment =
                Assessment.record(anAssessor().id(), true, proposing("8000.00"), COMMENT, REQUESTED, NOW);

        // then
        assertThat(assessment).isEqualTo(new Assessment(anAssessor().id(), true, proposing("8000.00"), COMMENT, NOW));
    }

    @Test
    @DisplayName("accepts a rejection recommendation for an ineligible application")
    void acceptsRejectionRecommendationForIneligibleApplication() {
        // when
        Assessment assessment =
                Assessment.record(anAssessor().id(), false, new Recommendation.Reject(), COMMENT, REQUESTED, NOW);

        // then
        assertThat(assessment.recommendation()).isEqualTo(new Recommendation.Reject());
    }

    @Test
    @DisplayName("rejects recommending an ineligible application for approval")
    void rejectsRecommendingIneligibleApplicationForApproval() {
        // when
        Throwable thrown = catchThrowable(
                () -> Assessment.record(anAssessor().id(), false, proposing("8000.00"), COMMENT, REQUESTED, NOW));

        // then
        assertThatDomainException(thrown)
                .isBusinessRuleViolation()
                .hasCode("APPROVAL_RECOMMENDATION_REQUIRES_ELIGIBILITY");
    }

    @Test
    @DisplayName("rejects proposing zero")
    void rejectsProposingZero() {
        // when
        Throwable thrown = catchThrowable(
                () -> Assessment.record(anAssessor().id(), true, proposing("0.00"), COMMENT, REQUESTED, NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROPOSED_AMOUNT_NOT_POSITIVE");
    }

    @Test
    @DisplayName("rejects proposing one cent more than the requested amount")
    void rejectsProposingOneCentMoreThanRequested() {
        // when
        Throwable thrown = catchThrowable(
                () -> Assessment.record(anAssessor().id(), true, proposing("8000.01"), COMMENT, REQUESTED, NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROPOSED_AMOUNT_EXCEEDS_REQUESTED");
    }

    @Test
    @DisplayName("strips whitespace around the comment")
    void stripsWhitespaceAroundTheComment() {
        // when
        Assessment assessment = Assessment.record(
                anAssessor().id(), false, new Recommendation.Reject(), "  " + COMMENT + "\n", REQUESTED, NOW);

        // then
        assertThat(assessment.comment()).isEqualTo(COMMENT);
    }

    @Test
    @DisplayName("rejects a blank comment")
    void rejectsBlankComment() {
        // when
        Throwable thrown = catchThrowable(
                () -> Assessment.record(anAssessor().id(), false, new Recommendation.Reject(), " ", REQUESTED, NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("ASSESSMENT_COMMENT_REQUIRED");
    }

    @Test
    @DisplayName("accepts a comment of exactly 4,000 characters")
    void acceptsCommentOfExactly4000Characters() {
        // given
        String longest = "a".repeat(4_000);

        // when
        Assessment assessment =
                Assessment.record(anAssessor().id(), false, new Recommendation.Reject(), longest, REQUESTED, NOW);

        // then
        assertThat(assessment.comment()).hasSize(4_000);
    }

    @Test
    @DisplayName("rejects a comment longer than 4,000 characters")
    void rejectsCommentLongerThan4000Characters() {
        // given
        String tooLong = "a".repeat(4_001);

        // when
        Throwable thrown = catchThrowable(() ->
                Assessment.record(anAssessor().id(), false, new Recommendation.Reject(), tooLong, REQUESTED, NOW));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("ASSESSMENT_COMMENT_TOO_LONG");
    }
}
