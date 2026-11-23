package be.achzak.civicgrant.core.organisation.application.service;

import static be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode.ORGANISATION_ALREADY_REGISTERED;

import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.error.ConflictException;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterOrganisationService implements RegisterOrganisationUseCase {

    private final OrganisationRepository organisations;
    private final DomainEventPublisher events;
    private final Clock clock;

    public RegisterOrganisationService(OrganisationRepository organisations, DomainEventPublisher events, Clock clock) {
        this.organisations = Objects.requireNonNull(organisations, "organisations");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public OrganisationId register(RegisterOrganisationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        if (organisations.findByEnterpriseNumber(command.enterpriseNumber()).isPresent()) {
            throw new ConflictException(
                    ORGANISATION_ALREADY_REGISTERED,
                    "Enterprise number " + command.enterpriseNumber().value() + " is already registered");
        }
        Organisation organisation = Organisation.create(
                command.enterpriseNumber(), command.name(), command.legalForm(), command.actor(), clock.instant());
        organisations.save(organisation);
        events.publish(organisation.pullEvents());
        return organisation.id();
    }
}
