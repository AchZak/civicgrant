package be.achzak.civicgrant.core.sharedkernel.application.port.out;

import be.achzak.civicgrant.core.sharedkernel.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {

    void publish(List<? extends DomainEvent> events);
}
