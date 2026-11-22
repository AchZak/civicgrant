package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import java.util.Objects;

public class SubmitApplicationService implements SubmitApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    public SubmitApplicationService(
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
    public void submit(SubmitApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications.getById(command.applicationId());
        // The foreign key guarantees the call exists, so a missing one is a bug.
        FundingCall fundingCall =
                fundingCalls.findById(application.fundingCallId()).orElseThrow();
        application.submit(fundingCall, command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
