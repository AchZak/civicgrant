package be.achzak.civicgrant.core.fundingcall.domain.event;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.domain.event.DomainEvent;

public sealed interface FundingCallEvent extends DomainEvent permits FundingCallOpened, GrantCommitted {

    FundingCallId fundingCallId();
}
