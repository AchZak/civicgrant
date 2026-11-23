package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordAssessmentService implements RecordAssessmentUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public void record(RecordAssessmentCommand command) {
        command.actor().requireAnyRole(Role.ASSESSOR);
        GrantApplication application = applications.getById(command.applicationId());
        application.recordAssessment(
                command.eligible(), command.recommendation(), command.comment(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
