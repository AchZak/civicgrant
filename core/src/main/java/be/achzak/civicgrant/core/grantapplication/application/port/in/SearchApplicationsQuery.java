package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Set;

public record SearchApplicationsQuery(
        Set<ApplicationStatus> statuses, Set<FundingCallId> fundingCalls, int page, int pageSize, Actor actor)
        implements Query {

    public static final int MAX_PAGE_SIZE = 100;

    public SearchApplicationsQuery {
        statuses = Set.copyOf(statuses);
        fundingCalls = Set.copyOf(fundingCalls);
        if (page < 1) {
            throw new IllegalArgumentException("Page " + page + " is below 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size " + pageSize + " is outside 1 to " + MAX_PAGE_SIZE);
        }
    }
}
