package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import java.time.Instant;

public record AssessmentRecorded(
        ApplicationId applicationId,
        boolean eligible,
        Recommendation recommendation,
        UserId actorId,
        Instant occurredAt)
        implements GrantApplicationEvent {}
