package be.achzak.civicgrant.core.fundingcall.domain.event;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;

public record GrantCommitted(FundingCallId fundingCallId, Money amount, UserId actorId, Instant occurredAt)
        implements FundingCallEvent {}
