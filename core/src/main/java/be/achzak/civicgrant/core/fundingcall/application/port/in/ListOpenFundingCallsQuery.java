package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record ListOpenFundingCallsQuery(Actor actor) implements Query {}
