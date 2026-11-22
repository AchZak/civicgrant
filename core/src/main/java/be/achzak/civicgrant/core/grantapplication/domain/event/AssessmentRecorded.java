package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;
import java.util.Objects;

public record AssessmentRecorded(
        ApplicationId applicationId,
        boolean eligible,
        Recommendation recommendation,
        UserId actorId,
        Instant occurredAt)
        implements GrantApplicationEvent {

    public AssessmentRecorded {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(recommendation, "recommendation");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
