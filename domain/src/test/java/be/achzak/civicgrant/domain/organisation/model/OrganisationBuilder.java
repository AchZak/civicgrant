package be.achzak.civicgrant.domain.organisation.model;

import java.time.Instant;

public final class OrganisationBuilder {

    private OrganisationId id = OrganisationId.generate();
    private EnterpriseNumber enterpriseNumber = new EnterpriseNumber("0123456749");
    private LegalForm legalForm = LegalForm.VZW_ASBL;

    private OrganisationBuilder() {}

    public static OrganisationBuilder anOrganisation() {
        return new OrganisationBuilder();
    }

    public OrganisationBuilder withId(OrganisationId id) {
        this.id = id;
        return this;
    }

    public OrganisationBuilder withEnterpriseNumber(EnterpriseNumber enterpriseNumber) {
        this.enterpriseNumber = enterpriseNumber;
        return this;
    }

    public OrganisationBuilder withLegalForm(LegalForm legalForm) {
        this.legalForm = legalForm;
        return this;
    }

    public Organisation build() {
        return Organisation.restore(
                id, enterpriseNumber, "Buurthuis De Linde", legalForm, Instant.parse("2026-01-15T09:00:00Z"));
    }
}
