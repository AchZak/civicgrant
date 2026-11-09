package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.UserId;
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
