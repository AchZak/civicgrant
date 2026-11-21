package be.achzak.civicgrant.domain.fundingcall.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.util.Objects;

public class OpenFundingCallService implements OpenFundingCallUseCase {

    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OpenFundingCallService(FundingCallRepository fundingCalls, DomainEventPublisher events, Clock clock) {
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // A reference that's already taken is refused by the repository's save, as FUNDING_CALL_REFERENCE_TAKEN.
    @Override
    public FundingCallId open(OpenFundingCallCommand command) {
        Objects.requireNonNull(command, "command");
        command.actor().requireAnyRole(Role.PROGRAMME_MANAGER);
        FundingCall fundingCall = FundingCall.create(
                command.reference(),
                command.title(),
                command.submissionPeriod(),
                command.totalBudget(),
                command.capPerApplication(),
                command.actor(),
                clock.instant());
        fundingCalls.save(fundingCall);
        events.publish(fundingCall.pullEvents());
        return fundingCall.id();
    }
}
