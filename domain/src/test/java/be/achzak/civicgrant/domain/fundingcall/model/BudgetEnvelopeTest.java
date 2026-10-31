package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.shared.Money.euros;
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
        @DisplayName("never lets the cap per application exceed the total")
        void capPerApplicationNeverExceedsTotal() {
            // given
            Money total = euros("50000.00");
            Money oneCentAboveTotal = euros("50000.01");

            // when
            Throwable thrown = catchThrowable(() -> new BudgetEnvelope(total, Money.ZERO, oneCentAboveTotal));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
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
