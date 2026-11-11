package be.achzak.civicgrant.domain.organisation.service;

import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.domain.organisation.port.in.GetOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.Objects;

public class GetOrganisationService implements GetOrganisationUseCase {

    private final OrganisationRepository organisations;

    public GetOrganisationService(OrganisationRepository organisations) {
        this.organisations = Objects.requireNonNull(organisations, "organisations");
    }

    @Override
    public Organisation get(GetOrganisationQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return organisations
                .findById(query.organisationId())
                .orElseThrow(() -> new NotFoundException(
                        "ORGANISATION_NOT_FOUND",
                        "Organisation " + query.organisationId().value() + " not found"));
    }
}
