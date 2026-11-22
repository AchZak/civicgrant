package be.achzak.civicgrant.domain.fundingcall.model.event;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.shared.event.DomainEvent;

public sealed interface FundingCallEvent extends DomainEvent permits FundingCallOpened, GrantCommitted {

    FundingCallId fundingCallId();
}
