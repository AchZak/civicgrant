package be.achzak.civicgrant.domain.grantapplication.port.out;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationVisibility;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.Set;

public interface GrantApplicationRepository {

    Optional<GrantApplication> findById(ApplicationId id);

    default GrantApplication getById(ApplicationId id) {
        return findById(id)
                .orElseThrow(() ->
                        new NotFoundException("APPLICATION_NOT_FOUND", "Application " + id.value() + " not found"));
    }

    void save(GrantApplication application);

    // Most recently changed first, then by id, so pages never overlap. An empty set doesn't filter.
    ApplicationPage search(
            ApplicationVisibility visibility,
            Set<ApplicationStatus> statuses,
            Set<FundingCallId> fundingCalls,
            int page,
            int pageSize);
}
