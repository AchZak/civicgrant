package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record ListOpenFundingCallsQuery(Actor actor) implements Query {

    public ListOpenFundingCallsQuery {
        Objects.requireNonNull(actor, "actor");
    }
}
