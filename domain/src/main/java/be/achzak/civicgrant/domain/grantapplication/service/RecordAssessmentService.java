package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.util.Objects;

public class RecordAssessmentService implements RecordAssessmentUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RecordAssessmentService(GrantApplicationRepository applications, DomainEventPublisher events, Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void record(RecordAssessmentCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.ASSESSOR);
        GrantApplication application = applications.getById(command.applicationId());
        application.recordAssessment(
                command.eligible(), command.recommendation(), command.comment(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
