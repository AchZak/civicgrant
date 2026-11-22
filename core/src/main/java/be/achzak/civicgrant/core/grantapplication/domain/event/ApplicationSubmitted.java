package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
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
