package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record FundingCallOpened(
        FundingCallId fundingCallId, String reference, Money totalBudget, UserId actorId, Instant occurredAt)
        implements FundingCallEvent {

    public FundingCallOpened {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(totalBudget, "totalBudget");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
