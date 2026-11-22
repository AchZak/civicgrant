package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Budget envelope")
class BudgetEnvelopeTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("never lets the committed amount exceed the total")
        void committedNeverExceedsTotal() {
            // given
            Money total = euros("50000.00");
            Money oneCentAboveTotal = euros("50000.01");

            // when
            Throwable thrown = catchThrowable(() -> new BudgetEnvelope(total, oneCentAboveTotal, euros("15000.00")));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a cap per application one cent above the total")
        void rejectsCapPerApplicationOneCentAboveTheTotal() {
            // given
            Money total = euros("50000.00");
            Money oneCentAboveTotal = euros("50000.01");

            // when
            Throwable thrown = catchThrowable(() -> new BudgetEnvelope(total, Money.ZERO, oneCentAboveTotal));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("CAP_PER_APPLICATION_EXCEEDS_BUDGET");
        }

        @Test
        @DisplayName("allows a cap per application of exactly the total")
        void allowsCapPerApplicationOfExactlyTheTotal() {
            // when
            BudgetEnvelope budget = BudgetEnvelope.unspent(euros("50000.00"), euros("50000.00"));

            // then
            assertThat(budget.capPerApplication()).isEqualTo(euros("50000.00"));
        }

        @Test
        @DisplayName("rejects a total budget of zero")
        void rejectsTotalBudgetOfZero() {
            // when
            Throwable thrown = catchThrowable(() -> BudgetEnvelope.unspent(Money.ZERO, euros("15000.00")));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("TOTAL_BUDGET_NOT_POSITIVE");
        }

        @Test
        @DisplayName("rejects a cap per application of zero")
        void rejectsCapPerApplicationOfZero() {
            // when
            Throwable thrown = catchThrowable(() -> BudgetEnvelope.unspent(euros("50000.00"), Money.ZERO));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("CAP_PER_APPLICATION_NOT_POSITIVE");
        }
    }

    @Nested
    @DisplayName("An unspent envelope")
    class Unspent {

        @Test
        @DisplayName("has nothing committed yet")
        void hasNothingCommittedYet() {
            // when
            BudgetEnvelope budget = BudgetEnvelope.unspent(euros("50000.00"), euros("15000.00"));

            // then
            assertThat(budget.committed()).isEqualTo(Money.ZERO);
        }
    }

    @Nested
    @DisplayName("Remaining budget")
    class Remaining {

        @Test
        @DisplayName("is the total minus what is already committed")
        void remainingIsTotalMinusCommitted() {
            // given
            BudgetEnvelope budget = new BudgetEnvelope(euros("50000.00"), euros("12500.50"), euros("15000.00"));

            // when
            Money remaining = budget.remaining();

            // then
            assertThat(remaining).isEqualTo(euros("37499.50"));
        }
    }
}
