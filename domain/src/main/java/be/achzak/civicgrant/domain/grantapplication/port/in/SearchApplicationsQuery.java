package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;
import java.util.Set;

public record SearchApplicationsQuery(
        Set<ApplicationStatus> statuses, Set<FundingCallId> fundingCalls, int page, int pageSize, Actor actor) {

    public static final int MAX_PAGE_SIZE = 100;

    public SearchApplicationsQuery {
        statuses = Set.copyOf(Objects.requireNonNull(statuses, "statuses"));
        fundingCalls = Set.copyOf(Objects.requireNonNull(fundingCalls, "fundingCalls"));
        Objects.requireNonNull(actor, "actor");
        if (page < 1) {
            throw new IllegalArgumentException("Page " + page + " is below 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size " + pageSize + " is outside 1 to " + MAX_PAGE_SIZE);
        }
    }
}
