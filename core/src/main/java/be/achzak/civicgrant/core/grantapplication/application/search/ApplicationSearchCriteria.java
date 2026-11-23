package be.achzak.civicgrant.core.grantapplication.application.search;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationVisibility;
import java.util.Set;

// An empty set doesn't filter: no statuses means every status.
public record ApplicationSearchCriteria(
        ApplicationVisibility visibility, Set<ApplicationStatus> statuses, Set<FundingCallId> fundingCalls) {

    public ApplicationSearchCriteria {
        statuses = Set.copyOf(statuses);
        fundingCalls = Set.copyOf(fundingCalls);
    }
}
