package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.util.Objects;

public class EditApplicationService implements EditApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    public EditApplicationService(
            GrantApplicationRepository applications,
            FundingCallRepository fundingCalls,
            DomainEventPublisher events,
            Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void edit(EditApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications
                .findById(command.applicationId())
                .orElseThrow(() -> new NotFoundException(
                        "APPLICATION_NOT_FOUND",
                        "Application " + command.applicationId().value() + " not found"));
        // The foreign key guarantees the call exists, so a missing one is a bug.
        FundingCall fundingCall =
                fundingCalls.findById(application.fundingCallId()).orElseThrow();
        application.edit(command.proposal(), fundingCall, command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
