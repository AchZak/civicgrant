package be.achzak.civicgrant.core.grantapplication.application.search;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationVisibility;
import java.util.Objects;
import java.util.Set;

// An empty set doesn't filter: no statuses means every status.
public record ApplicationSearchCriteria(
        ApplicationVisibility visibility, Set<ApplicationStatus> statuses, Set<FundingCallId> fundingCalls) {

    public ApplicationSearchCriteria {
        Objects.requireNonNull(visibility, "visibility");
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses"));
        fundingCalls = Set.copyOf(Objects.requireNonNull(fundingCalls, "fundingCalls"));
    }
}
