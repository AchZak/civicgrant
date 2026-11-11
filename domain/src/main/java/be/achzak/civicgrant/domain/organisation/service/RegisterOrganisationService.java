package be.achzak.civicgrant.domain.organisation.service;

import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.util.Objects;

public class RegisterOrganisationService implements RegisterOrganisationUseCase {

    private final OrganisationRepository organisations;
    private final Clock clock;

    public RegisterOrganisationService(OrganisationRepository organisations, Clock clock) {
        this.organisations = Objects.requireNonNull(organisations, "organisations");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public OrganisationId register(RegisterOrganisationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        if (organisations.findByEnterpriseNumber(command.enterpriseNumber()).isPresent()) {
            throw new ConflictException(
                    "ORGANISATION_ALREADY_REGISTERED",
                    "Enterprise number " + command.enterpriseNumber().value() + " is already registered");
        }
        Organisation organisation = Organisation.create(
                command.enterpriseNumber(), command.name(), command.legalForm(), command.actor(), clock.instant());
        organisations.save(organisation);
        return organisation.id();
    }
}
