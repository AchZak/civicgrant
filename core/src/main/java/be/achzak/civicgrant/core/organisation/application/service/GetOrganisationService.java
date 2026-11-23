package be.achzak.civicgrant.core.organisation.application.service;

import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrganisationService implements GetOrganisationUseCase {

    private final OrganisationRepository organisations;

    public GetOrganisationService(OrganisationRepository organisations) {
        this.organisations = organisations;
    }

    @Override
    @Transactional(readOnly = true)
    public Organisation get(GetOrganisationQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return organisations.getById(query.organisationId());
    }
}
