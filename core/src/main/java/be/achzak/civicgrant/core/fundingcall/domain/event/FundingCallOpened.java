package be.achzak.civicgrant.core.fundingcall.domain.event;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;

public record FundingCallOpened(
        FundingCallId fundingCallId,
        FundingCallReference reference,
        Money totalBudget,
        UserId actorId,
        Instant occurredAt)
        implements FundingCallEvent {}
