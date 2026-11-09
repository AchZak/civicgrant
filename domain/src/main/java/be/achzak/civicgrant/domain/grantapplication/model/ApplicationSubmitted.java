package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record ApplicationSubmitted(
        ApplicationId applicationId, Money requestedAmount, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {

    public ApplicationSubmitted {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(requestedAmount, "requestedAmount");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
