package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveApplicationService implements ApproveApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public void approve(ApproveApplicationCommand command) {
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
