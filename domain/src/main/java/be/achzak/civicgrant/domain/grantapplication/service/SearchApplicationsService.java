package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSearchCriteria;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationVisibility;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.Objects;

public class SearchApplicationsService implements SearchApplicationsUseCase {

    private final GrantApplicationRepository applications;

    public SearchApplicationsService(GrantApplicationRepository applications) {
        this.applications = Objects.requireNonNull(applications, "applications");
    }

    @Override
    public ApplicationPage search(SearchApplicationsQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria(
                ApplicationVisibility.of(query.actor()), query.statuses(), query.fundingCalls());
        return applications.search(criteria, query.page(), query.pageSize());
    }
}
