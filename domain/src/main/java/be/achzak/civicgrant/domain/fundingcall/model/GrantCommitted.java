package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record GrantCommitted(FundingCallId fundingCallId, Money amount, UserId actorId, Instant occurredAt)
        implements FundingCallEvent {

    public GrantCommitted {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
