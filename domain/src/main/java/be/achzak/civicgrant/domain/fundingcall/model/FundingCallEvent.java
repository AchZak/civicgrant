package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.DomainEvent;

public sealed interface FundingCallEvent extends DomainEvent permits GrantCommitted {

    FundingCallId fundingCallId();
}
