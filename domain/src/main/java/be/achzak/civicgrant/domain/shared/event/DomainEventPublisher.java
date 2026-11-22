package be.achzak.civicgrant.domain.shared.event;

import java.util.List;

public interface DomainEventPublisher {

    void publish(List<? extends DomainEvent> events);
}
