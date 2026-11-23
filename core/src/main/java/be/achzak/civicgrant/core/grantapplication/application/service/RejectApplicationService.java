package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectApplicationService implements RejectApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RejectApplicationService(GrantApplicationRepository applications, DomainEventPublisher events, Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void reject(RejectApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.DECISION_MAKER);
        GrantApplication application = applications.getById(command.applicationId());
        application.reject(command.ground(), command.motivation(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
