package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.UserId;
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
