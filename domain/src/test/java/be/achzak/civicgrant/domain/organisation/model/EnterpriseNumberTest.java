package be.achzak.civicgrant.domain.organisation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Enterprise number")
class EnterpriseNumberTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("accepts a number whose last two digits are 97 minus the first eight digits mod 97")
        void acceptsNumberWhoseLastTwoDigitsAre97MinusFirstEightMod97() {
            // when
            EnterpriseNumber number = new EnterpriseNumber("0123456749");

            // then
            assertThat(number.value()).isEqualTo("0123456749");
        }

        @Test
        @DisplayName("accepts a number starting with 1")
        void acceptsNumberStartingWithOne() {
            // when
            EnterpriseNumber number = new EnterpriseNumber("1000000021");

            // then
            assertThat(number.value()).isEqualTo("1000000021");
        }

        @Test
        @DisplayName("accepts check digits 97 when the first eight digits are a multiple of 97")
        void acceptsCheckDigits97WhenFirstEightDigitsAreMultipleOf97() {
            // when
            EnterpriseNumber number = new EnterpriseNumber("0123510197");

            // then
            assertThat(number.value()).isEqualTo("0123510197");
        }

        @Test
        @DisplayName("rejects a number not starting with 0 or 1")
        void rejectsNumberNotStartingWithZeroOrOne() {
            // when
            Throwable thrown = catchThrowable(() -> new EnterpriseNumber("2123456749"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ENTERPRISE_NUMBER_INVALID_FORMAT");
        }

        @Test
        @DisplayName("rejects a number without exactly ten digits")
        void rejectsNumberWithoutTenDigits() {
            // when
            Throwable thrown = catchThrowable(() -> new EnterpriseNumber("012345674"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ENTERPRISE_NUMBER_INVALID_FORMAT");
        }

        @Test
        @DisplayName("rejects a number containing letters")
        void rejectsNumberWithLetters() {
            // when
            Throwable thrown = catchThrowable(() -> new EnterpriseNumber("01234567A9"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ENTERPRISE_NUMBER_INVALID_FORMAT");
        }

        @Test
        @DisplayName("rejects a number with wrong check digits")
        void rejectsNumberWithWrongCheckDigits() {
            // when
            Throwable thrown = catchThrowable(() -> new EnterpriseNumber("0123456748"));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ENTERPRISE_NUMBER_INVALID_CHECKSUM");
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parse {

        @Test
        @DisplayName("accepts the usual notation with dots and surrounding spaces")
        void acceptsTheUsualNotationWithDots() {
            // given
            String input = " 0123.456.749 ";

            // when
            EnterpriseNumber number = EnterpriseNumber.parse(input);

            // then
            assertThat(number).isEqualTo(new EnterpriseNumber("0123456749"));
        }

        @Test
        @DisplayName("accepts the VAT notation with a BE prefix in any case")
        void acceptsTheVatNotationWithBePrefix() {
            // given
            String input = "be 0123.456.749";

            // when
            EnterpriseNumber number = EnterpriseNumber.parse(input);

            // then
            assertThat(number).isEqualTo(new EnterpriseNumber("0123456749"));
        }
    }
}
