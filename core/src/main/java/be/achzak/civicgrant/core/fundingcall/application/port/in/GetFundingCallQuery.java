package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record GetFundingCallQuery(FundingCallId fundingCallId, Actor actor) implements Query {

    public GetFundingCallQuery {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(actor, "actor");
    }
}
