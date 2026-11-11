package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record GetFundingCallQuery(FundingCallId fundingCallId, Actor actor) {

    public GetFundingCallQuery {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(actor, "actor");
    }
}
