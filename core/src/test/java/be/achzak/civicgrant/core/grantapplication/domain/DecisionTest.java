package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.aDecisionMaker;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Decision")
class DecisionTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    @Nested
    @DisplayName("Approval")
    class Approving {

        private static final Money REQUESTED = euros("8000.00");
        private static final String MOTIVATION = "Het project versterkt de buurt en de begroting is onderbouwd.";

        @Test
        @DisplayName("grants exactly the requested amount")
        void grantsExactlyTheRequestedAmount() {
            // when
            Decision.Approval approval = Decision.approval(
                    euros("8000.00"), REQUESTED, aDecisionMaker().id(), MOTIVATION, NOW);

            // then
            assertThat(approval)
                    .isEqualTo(new Decision.Approval(
                            euros("8000.00"), aDecisionMaker().id(), MOTIVATION, NOW));
        }

        @Test
        @DisplayName("may grant less than the requested amount")
        void mayGrantLessThanTheRequestedAmount() {
            // when
            Decision.Approval approval = Decision.approval(
                    euros("5000.00"), REQUESTED, aDecisionMaker().id(), MOTIVATION, NOW);

            // then
            assertThat(approval.grantedAmount()).isEqualTo(euros("5000.00"));
        }

        @Test
        @DisplayName("rejects granting zero")
        void rejectsGrantingZero() {
            // when
            Throwable thrown = catchThrowable(() ->
                    Decision.approval(euros("0.00"), REQUESTED, aDecisionMaker().id(), MOTIVATION, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANTED_AMOUNT_NOT_POSITIVE");
        }

        @Test
        @DisplayName("rejects granting one cent more than the requested amount")
        void rejectsGrantingOneCentMoreThanRequested() {
            // when
            Throwable thrown = catchThrowable(() -> Decision.approval(
                    euros("8000.01"), REQUESTED, aDecisionMaker().id(), MOTIVATION, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANTED_AMOUNT_EXCEEDS_REQUESTED");
        }

        @Test
        @DisplayName("strips whitespace around the motivation")
        void stripsWhitespaceAroundTheMotivation() {
            // when
            Decision.Approval approval = Decision.approval(
                    euros("8000.00"), REQUESTED, aDecisionMaker().id(), "  " + MOTIVATION + "\n", NOW);

            // then
            assertThat(approval.motivation()).isEqualTo(MOTIVATION);
        }

        @Test
        @DisplayName("rejects a blank motivation")
        void rejectsBlankMotivation() {
            // when
            Throwable thrown = catchThrowable(() -> Decision.approval(
                    euros("8000.00"), REQUESTED, aDecisionMaker().id(), "  ", NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_REQUIRED");
        }

        @Test
        @DisplayName("accepts a motivation of exactly 4,000 characters")
        void acceptsMotivationOfExactly4000Characters() {
            // given
            String longest = "a".repeat(4_000);

            // when
            Decision.Approval approval = Decision.approval(
                    euros("8000.00"), REQUESTED, aDecisionMaker().id(), longest, NOW);

            // then
            assertThat(approval.motivation()).hasSize(4_000);
        }

        @Test
        @DisplayName("rejects a motivation longer than 4,000 characters")
        void rejectsMotivationLongerThan4000Characters() {
            // given
            String tooLong = "a".repeat(4_001);

            // when
            Throwable thrown = catchThrowable(() -> Decision.approval(
                    euros("8000.00"), REQUESTED, aDecisionMaker().id(), tooLong, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_TOO_LONG");
        }
    }

    @Nested
    @DisplayName("Rejection")
    class Rejecting {

        private static final String MOTIVATION = "Het projectplan is onvoldoende uitgewerkt.";

        @Test
        @DisplayName("keeps the ground and the stripped motivation")
        void keepsTheGroundAndTheStrippedMotivation() {
            // when
            Decision.Rejection rejection = Decision.rejection(
                    RejectionGround.INCOMPLETE, aDecisionMaker().id(), " " + MOTIVATION + " ", NOW);

            // then
            assertThat(rejection)
                    .isEqualTo(new Decision.Rejection(
                            RejectionGround.INCOMPLETE, aDecisionMaker().id(), MOTIVATION, NOW));
        }

        @Test
        @DisplayName("rejects a blank motivation")
        void rejectsBlankMotivation() {
            // when
            Throwable thrown = catchThrowable(() -> Decision.rejection(
                    RejectionGround.INCOMPLETE, aDecisionMaker().id(), "\t", NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_REQUIRED");
        }

        @Test
        @DisplayName("rejects a motivation longer than 4,000 characters")
        void rejectsMotivationLongerThan4000Characters() {
            // given
            String tooLong = "a".repeat(4_001);

            // when
            Throwable thrown = catchThrowable(() -> Decision.rejection(
                    RejectionGround.INCOMPLETE, aDecisionMaker().id(), tooLong, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("MOTIVATION_TOO_LONG");
        }
    }
}
