package be.achzak.civicgrant.core.common.application.port.out;

import be.achzak.civicgrant.core.common.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {

    void publish(List<? extends DomainEvent> events);
}
