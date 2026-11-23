package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;

public record DocumentRemoved(ApplicationId applicationId, DocumentId documentId, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {}
