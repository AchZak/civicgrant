package be.achzak.civicgrant.core.grantapplication.application.port.out;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    @Override
    public ApplicationPage search(ApplicationSearchCriteria criteria, int page, int pageSize) {
        Set<ApplicationStatus> statuses = criteria.statuses();
        Set<FundingCallId> fundingCalls = criteria.fundingCalls();
        List<ApplicationSummary> matching = applications.values().stream()
                .filter(application -> criteria.visibility().allows(application.applicant(), application.status()))
                .filter(application -> statuses.isEmpty() || statuses.contains(application.status()))
                .filter(application -> fundingCalls.isEmpty() || fundingCalls.contains(application.fundingCallId()))
                .sorted(Comparator.comparing(GrantApplication::updatedAt)
                        .reversed()
                        .thenComparing(application -> application.id().value()))
                .map(InMemoryGrantApplicationRepository::summaryOf)
                .toList();
        List<ApplicationSummary> items = matching.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .toList();
        return new ApplicationPage(items, page, pageSize, matching.size());
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
