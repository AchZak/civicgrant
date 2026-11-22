package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.LEGAL_FORM_NOT_ELIGIBLE;

import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.sharedkernel.domain.error.BusinessRuleViolationException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record EligibilityPolicy(Set<LegalForm> eligibleForms) {

    public EligibilityPolicy {
        Objects.requireNonNull(eligibleForms, "eligibleForms");
        if (eligibleForms.isEmpty()) {
            throw new IllegalArgumentException("An eligibility policy admits at least one legal form");
        }
        eligibleForms = Set.copyOf(eligibleForms);
    }

    public static EligibilityPolicy nonProfits() {
        return new EligibilityPolicy(EnumSet.of(LegalForm.VZW_ASBL, LegalForm.IVZW_AISBL, LegalForm.FOUNDATION));
    }

    public void ensureEligible(Organisation organisation) {
        Objects.requireNonNull(organisation, "organisation");
        if (!eligibleForms.contains(organisation.legalForm())) {
            throw new BusinessRuleViolationException(
                    LEGAL_FORM_NOT_ELIGIBLE,
                    "Legal form " + organisation.legalForm() + " of organisation "
                            + organisation.id().value() + " isn't eligible");
        }
    }
}
