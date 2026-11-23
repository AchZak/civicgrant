package be.achzak.civicgrant.core.common.domain.event;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import java.time.Instant;

public interface DomainEvent {

    UserId actorId();

    Instant occurredAt();
}
