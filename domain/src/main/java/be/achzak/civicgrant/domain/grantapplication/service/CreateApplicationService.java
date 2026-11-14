package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.EligibilityPolicy;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.ApplicationReferenceSequence;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.Objects;

public class CreateApplicationService implements CreateApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final OrganisationRepository organisations;
    private final ApplicationReferenceSequence references;
    private final DomainEventPublisher events;
    private final Clock clock;

    public CreateApplicationService(
            GrantApplicationRepository applications,
            FundingCallRepository fundingCalls,
            OrganisationRepository organisations,
            ApplicationReferenceSequence references,
            DomainEventPublisher events,
            Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
        this.organisations = Objects.requireNonNull(organisations, "organisations");
        this.references = Objects.requireNonNull(references, "references");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ApplicationId create(CreateApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        FundingCall fundingCall = fundingCalls
                .findById(command.fundingCallId())
                .orElseThrow(() -> new NotFoundException(
                        "FUNDING_CALL_NOT_FOUND",
                        "Funding call " + command.fundingCallId().value() + " not found"));
        Organisation organisation = organisations
                .findById(command.organisationId())
                .orElseThrow(() -> new NotFoundException(
                        "ORGANISATION_NOT_FOUND",
                        "Organisation " + command.organisationId().value() + " not found"));
        Instant now = clock.instant();
        ApplicationReference reference =
                ApplicationReference.of(Year.from(now.atZone(ZoneOffset.UTC)), references.nextNumber());
        GrantApplication application = GrantApplication.create(
                reference,
                fundingCall,
                organisation,
                command.proposal(),
                EligibilityPolicy.nonProfits(),
                command.actor(),
                now);
        applications.save(application);
        events.publish(application.pullEvents());
        return application.id();
    }
}
