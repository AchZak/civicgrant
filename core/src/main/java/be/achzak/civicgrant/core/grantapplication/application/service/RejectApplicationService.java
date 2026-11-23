package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.common.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectApplicationService implements RejectApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public void reject(RejectApplicationCommand command) {
        command.actor().requireAnyRole(Role.DECISION_MAKER);
        GrantApplication application = applications.getById(command.applicationId());
        application.reject(command.ground(), command.motivation(), command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
