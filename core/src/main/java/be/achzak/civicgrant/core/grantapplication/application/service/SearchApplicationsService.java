package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SearchApplicationsUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationVisibility;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchApplicationsService implements SearchApplicationsUseCase {

    private final GrantApplicationRepository applications;

    @Override
    @Transactional(readOnly = true)
    public ApplicationPage search(SearchApplicationsQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria(
                ApplicationVisibility.of(query.actor()), query.statuses(), query.fundingCalls());
        return applications.search(criteria, query.page(), query.pageSize());
    }
}
