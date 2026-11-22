package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;
import java.util.Objects;

public record DocumentRemoved(ApplicationId applicationId, DocumentId documentId, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {

    public DocumentRemoved {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
