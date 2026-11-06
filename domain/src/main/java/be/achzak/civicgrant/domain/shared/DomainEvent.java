package be.achzak.civicgrant.domain.shared;

import java.time.Instant;

public interface DomainEvent {

    UserId actorId();

    Instant occurredAt();
}
