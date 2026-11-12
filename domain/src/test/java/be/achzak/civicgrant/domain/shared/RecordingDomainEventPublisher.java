package be.achzak.civicgrant.domain.shared;

import java.util.ArrayList;
import java.util.List;

public final class RecordingDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<? extends DomainEvent> events) {
        published.addAll(events);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }
}
