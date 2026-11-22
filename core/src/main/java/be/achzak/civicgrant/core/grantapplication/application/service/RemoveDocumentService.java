package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
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
