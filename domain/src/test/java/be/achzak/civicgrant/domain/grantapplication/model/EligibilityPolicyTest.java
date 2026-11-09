package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Eligibility policy")
class EligibilityPolicyTest {

    @Nested
    @DisplayName("By default")
    class NonProfits {

        private final EligibilityPolicy policy = EligibilityPolicy.nonProfits();

        @ParameterizedTest(name = "{0}")
        @EnumSource(
                value = LegalForm.class,
                names = {"VZW_ASBL", "IVZW_AISBL", "FOUNDATION"})
        @DisplayName("admits a non-profit legal form")
        void admitsNonProfitLegalForm(LegalForm legalForm) {
            // given
            Organisation organisation =
                    anOrganisation().withLegalForm(legalForm).build();

            // when
            Throwable thrown = catchThrowable(() -> policy.ensureEligible(organisation));

            // then
            assertThat(thrown).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(
                value = LegalForm.class,
                names = {"COMPANY", "PUBLIC_BODY"})
        @DisplayName("refuses any other legal form")
        void refusesAnyOtherLegalForm(LegalForm legalForm) {
            // given
            Organisation organisation =
                    anOrganisation().withLegalForm(legalForm).build();

            // when
            Throwable thrown = catchThrowable(() -> policy.ensureEligible(organisation));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("LEGAL_FORM_NOT_ELIGIBLE");
        }
    }

    @Nested
    @DisplayName("Configured")
    class Configured {

        @Test
        @DisplayName("admits exactly the legal forms it was given")
        void admitsExactlyTheLegalFormsItWasGiven() {
            // given
            EligibilityPolicy policy = new EligibilityPolicy(Set.of(LegalForm.COMPANY));
            Organisation company =
                    anOrganisation().withLegalForm(LegalForm.COMPANY).build();

            // when
            Throwable thrown = catchThrowable(() -> policy.ensureEligible(company));

            // then
            assertThat(thrown).isNull();
        }

        @Test
        @DisplayName("admits at least one legal form")
        void admitsAtLeastOneLegalForm() {
            // when
            Throwable thrown = catchThrowable(() -> new EligibilityPolicy(Set.of()));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
