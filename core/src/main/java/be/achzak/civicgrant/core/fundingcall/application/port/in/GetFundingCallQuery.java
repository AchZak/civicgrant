package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Query;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;

public record GetFundingCallQuery(FundingCallId fundingCallId, Actor actor) implements Query {}
