package be.achzak.civicgrant.app.event;

import be.achzak.civicgrant.core.common.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.event.DomainEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(List<? extends DomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }
}
