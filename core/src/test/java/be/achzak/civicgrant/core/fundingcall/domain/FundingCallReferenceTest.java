package be.achzak.civicgrant.core.fundingcall.domain;

import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Funding call reference")
class FundingCallReferenceTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"GRN-2027", "NBH-2026-B", "2027", "A"})
        @DisplayName("accepts capitals and digits in groups joined by single hyphens")
        void acceptsCapitalsAndDigitsInGroupsJoinedBySingleHyphens(String value) {
            // when
            FundingCallReference reference = new FundingCallReference(value);

            // then
            assertThat(reference.value()).isEqualTo(value);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"grn-2027", "GRN 2027", "GRN--2027", "-GRN", "GRN-", "GRN_2027", " GRN-2027", ""})
        @DisplayName("rejects a reference that isn't capitals and digits in groups joined by single hyphens")
        void rejectsReferenceThatIsntCapitalsAndDigitsJoinedByHyphens(String value) {
            // when
            Throwable thrown = catchThrowable(() -> new FundingCallReference(value));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_REFERENCE_INVALID");
        }

        @Test
        @DisplayName("accepts a reference of exactly 30 characters")
        void acceptsReferenceOfExactly30Characters() {
            // given
            String thirtyCharacters = "GRN-" + "A".repeat(26);

            // when
            FundingCallReference reference = new FundingCallReference(thirtyCharacters);

            // then
            assertThat(reference.value()).hasSize(30);
        }

        @Test
        @DisplayName("rejects a reference of 31 characters")
        void rejectsReferenceOf31Characters() {
            // given
            String thirtyOneCharacters = "GRN-" + "A".repeat(27);

            // when
            Throwable thrown = catchThrowable(() -> new FundingCallReference(thirtyOneCharacters));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_REFERENCE_INVALID");
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parse {

        @Test
        @DisplayName("trims surrounding spaces")
        void trimsSurroundingSpaces() {
            // given
            String input = "  GRN-2027 ";

            // when
            FundingCallReference reference = FundingCallReference.parse(input);

            // then
            assertThat(reference).isEqualTo(new FundingCallReference("GRN-2027"));
        }

        @Test
        @DisplayName("rejects a reference that is only spaces")
        void rejectsReferenceThatIsOnlySpaces() {
            // when
            Throwable thrown = catchThrowable(() -> FundingCallReference.parse("   "));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_REFERENCE_INVALID");
        }
    }
}
