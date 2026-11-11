package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record ListOpenFundingCallsQuery(Actor actor) {

    public ListOpenFundingCallsQuery {
        Objects.requireNonNull(actor, "actor");
    }
}
