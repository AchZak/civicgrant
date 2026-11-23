package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.common.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.EditApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditApplicationService implements EditApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public void edit(EditApplicationCommand command) {
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications.getById(command.applicationId());
        // The foreign key guarantees the call exists, so a missing one is a bug.
        FundingCall fundingCall =
                fundingCalls.findById(application.fundingCallId()).orElseThrow();
        application.edit(command.proposal(), fundingCall, command.actor(), clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
    }
}
