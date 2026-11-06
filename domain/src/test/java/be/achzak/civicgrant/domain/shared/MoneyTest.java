package be.achzak.civicgrant.domain.shared;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money")
class MoneyTest {

    @Nested
    @DisplayName("Creating an amount")
    class Create {

        @Test
        @DisplayName("rejects a negative amount")
        void rejectsNegativeAmount() {
            // when
            Throwable thrown = catchThrowable(() -> euros("-0.01"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "AMOUNT_NEGATIVE");
        }

        @Test
        @DisplayName("rejects more than two decimals")
        void rejectsMoreThanTwoDecimals() {
            // when
            Throwable thrown = catchThrowable(() -> euros("10.001"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "AMOUNT_TOO_PRECISE");
        }

        @Test
        @DisplayName("accepts trailing zeros beyond two decimals")
        void acceptsTrailingZerosBeyondTwoDecimals() {
            // when
            Money amount = euros("10.500");

            // then
            assertThat(amount).isEqualTo(euros("10.50"));
        }

        @Test
        @DisplayName("always has two decimals")
        void alwaysHasTwoDecimals() {
            // when
            Money amount = euros("10");

            // then
            assertThat(amount.amount()).isEqualTo(new BigDecimal("10.00"));
        }
    }

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("adds and subtracts")
        void addsAndSubtracts() {
            // given
            Money hundred = euros("100.00");

            // when
            Money result = hundred.plus(euros("0.50")).minus(euros("20.25"));

            // then
            assertThat(result).isEqualTo(euros("80.25"));
        }

        @Test
        @DisplayName("can't be subtracted below zero")
        void cantBeSubtractedBelowZero() {
            // given
            Money five = euros("5.00");

            // when
            Throwable thrown = catchThrowable(() -> five.minus(euros("5.01")));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "AMOUNT_NEGATIVE");
        }
    }
}
