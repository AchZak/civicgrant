package be.achzak.civicgrant.domain.fundingcall.model;

import java.util.Objects;
import java.util.UUID;

public record FundingCallId(UUID value) {

    public FundingCallId {
        Objects.requireNonNull(value, "value");
    }

    public static FundingCallId generate() {
        return new FundingCallId(UUID.randomUUID());
    }
}
