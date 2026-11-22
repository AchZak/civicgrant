package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.event.DomainEventPublisher;
import java.time.Clock;
import java.util.Objects;

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
    public void reject(RejectApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.DECISION_MAKER);
        GrantApplication application = applications.getById(command.applicationId());
        application.reject(command.ground(), command.motivation(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
