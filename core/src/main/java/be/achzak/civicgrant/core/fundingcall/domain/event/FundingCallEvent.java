package be.achzak.civicgrant.core.fundingcall.domain.event;

import be.achzak.civicgrant.core.common.domain.event.DomainEvent;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;

public sealed interface FundingCallEvent extends DomainEvent permits FundingCallOpened, GrantCommitted {

    FundingCallId fundingCallId();
}
