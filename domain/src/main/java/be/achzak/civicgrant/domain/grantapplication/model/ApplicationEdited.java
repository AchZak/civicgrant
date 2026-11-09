package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record ApplicationEdited(ApplicationId applicationId, Money requestedAmount, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {

    public ApplicationEdited {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(requestedAmount, "requestedAmount");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
