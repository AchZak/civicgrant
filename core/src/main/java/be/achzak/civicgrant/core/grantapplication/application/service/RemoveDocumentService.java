package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveDocumentService implements RemoveDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RemoveDocumentService(GrantApplicationRepository applications, DomainEventPublisher events, Clock clock) {
        this.applications = applications;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void remove(RemoveDocumentCommand command) {
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications.getById(command.applicationId());
        application.removeDocument(command.documentId(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
