package be.achzak.civicgrant.core.fundingcall.domain.event;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
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
