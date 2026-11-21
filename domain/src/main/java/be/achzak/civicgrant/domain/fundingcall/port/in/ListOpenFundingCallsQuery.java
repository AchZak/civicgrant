package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Query;
import java.util.Objects;

public record ListOpenFundingCallsQuery(Actor actor) implements Query {

    public ListOpenFundingCallsQuery {
        Objects.requireNonNull(actor, "actor");
    }
}
