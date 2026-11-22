package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.event.DomainEventPublisher;
import java.time.Clock;
import java.util.Objects;

public class RemoveDocumentService implements RemoveDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RemoveDocumentService(GrantApplicationRepository applications, DomainEventPublisher events, Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void remove(RemoveDocumentCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications.getById(command.applicationId());
        application.removeDocument(command.documentId(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
