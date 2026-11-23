package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Query;
import be.achzak.civicgrant.core.common.domain.identity.Actor;

public record ListOpenFundingCallsQuery(Actor actor) implements Query {}
