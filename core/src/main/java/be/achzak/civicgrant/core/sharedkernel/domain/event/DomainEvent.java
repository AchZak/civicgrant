package be.achzak.civicgrant.core.sharedkernel.domain.event;

import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;

public interface DomainEvent {

    UserId actorId();

    Instant occurredAt();
}
