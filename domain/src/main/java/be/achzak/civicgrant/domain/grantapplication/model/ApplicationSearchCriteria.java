package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
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
