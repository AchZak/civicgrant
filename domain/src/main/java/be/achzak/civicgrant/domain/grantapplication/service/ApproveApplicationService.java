package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class ApproveApplicationService implements ApproveApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    public ApproveApplicationService(
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
    public void approve(ApproveApplicationCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.DECISION_MAKER);
        GrantApplication application = applications.getById(command.applicationId());
        // The foreign key guarantees the call exists, so a missing one is a bug.
        FundingCall fundingCall =
                fundingCalls.findById(application.fundingCallId()).orElseThrow();
        Instant now = clock.instant();
        application.approve(command.grantedAmount(), command.motivation(), command.actor(), now);
        fundingCall.commitGrant(command.grantedAmount(), command.actor(), now);
        applications.save(application);
        fundingCalls.save(fundingCall);
        events.publish(application.pullEvents());
        events.publish(fundingCall.pullEvents());
    }
}
