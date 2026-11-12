package be.achzak.civicgrant;

import be.achzak.civicgrant.domain.shared.DomainEvent;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(List<? extends DomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }
}
