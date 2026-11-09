package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record ApplicationApproved(ApplicationId applicationId, Money grantedAmount, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {

    public ApplicationApproved {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(grantedAmount, "grantedAmount");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
