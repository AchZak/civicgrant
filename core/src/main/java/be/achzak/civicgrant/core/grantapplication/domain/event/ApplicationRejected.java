package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;
import java.util.Objects;

public record ApplicationRejected(
        ApplicationId applicationId, RejectionGround ground, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {

    public ApplicationRejected {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(ground, "ground");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
