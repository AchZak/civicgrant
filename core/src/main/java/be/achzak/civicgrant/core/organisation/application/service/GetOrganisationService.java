package be.achzak.civicgrant.core.organisation.application.service;

import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
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
        return organisations.getById(query.organisationId());
    }
}
