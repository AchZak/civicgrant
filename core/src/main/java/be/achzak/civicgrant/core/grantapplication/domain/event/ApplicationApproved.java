package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
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
