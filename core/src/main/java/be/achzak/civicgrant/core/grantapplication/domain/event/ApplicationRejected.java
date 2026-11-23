package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import java.time.Instant;

public record ApplicationRejected(
        ApplicationId applicationId, RejectionGround ground, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {}
