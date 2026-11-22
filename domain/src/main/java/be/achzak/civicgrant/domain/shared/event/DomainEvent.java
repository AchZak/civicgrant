package be.achzak.civicgrant.domain.shared.event;

import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;

public interface DomainEvent {

    UserId actorId();

    Instant occurredAt();
}
