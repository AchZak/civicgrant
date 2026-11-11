package be.achzak.civicgrant.domain.organisation.service;

import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.List;
import java.util.Objects;

public class SearchOrganisationsService implements SearchOrganisationsUseCase {

    private final OrganisationRepository organisations;

    public SearchOrganisationsService(OrganisationRepository organisations) {
        this.organisations = Objects.requireNonNull(organisations, "organisations");
    }

    @Override
    public List<Organisation> search(SearchOrganisationsQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return organisations.findByEnterpriseNumber(query.enterpriseNumber()).stream()
                .toList();
    }
}
