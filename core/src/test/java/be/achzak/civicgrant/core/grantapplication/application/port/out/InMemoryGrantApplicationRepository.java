package be.achzak.civicgrant.core.grantapplication.application.port.out;

import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryGrantApplicationRepository implements GrantApplicationRepository {

    private final Map<ApplicationId, GrantApplication> applications = new LinkedHashMap<>();

    public InMemoryGrantApplicationRepository(GrantApplication... applications) {
        for (GrantApplication application : applications) {
            this.applications.put(application.id(), application);
        }
    }

    @Override
    public Optional<GrantApplication> findById(ApplicationId id) {
        return Optional.ofNullable(applications.get(id));
    }

    @Override
    public void save(GrantApplication application) {
        applications.put(application.id(), application);
    }

    // Visibility only: the filters, the order and the paging are SQL, tested in persistence.
    @Override
    public ApplicationPage search(ApplicationSearchCriteria criteria, int page, int pageSize) {
        List<ApplicationSummary> visible = applications.values().stream()
                .filter(application -> criteria.visibility().allows(application.applicant(), application.status()))
                .map(InMemoryGrantApplicationRepository::summaryOf)
                .toList();
        return new ApplicationPage(visible, page, pageSize, visible.size());
    }

    private static ApplicationSummary summaryOf(GrantApplication application) {
        return new ApplicationSummary(
                application.id(),
                application.reference(),
                application.fundingCallId(),
                application.organisationId(),
                application.status(),
                application.proposal().title(),
                application.proposal().requestedAmount(),
                application.submittedAt().orElse(null),
                application.updatedAt());
    }
}
