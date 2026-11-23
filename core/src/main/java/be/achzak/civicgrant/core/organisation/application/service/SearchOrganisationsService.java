package be.achzak.civicgrant.core.organisation.application.service;

import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchOrganisationsService implements SearchOrganisationsUseCase {

    private final OrganisationRepository organisations;

    @Override
    @Transactional(readOnly = true)
    public List<Organisation> search(SearchOrganisationsQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return organisations.findByEnterpriseNumber(query.enterpriseNumber()).stream()
                .toList();
    }
}
