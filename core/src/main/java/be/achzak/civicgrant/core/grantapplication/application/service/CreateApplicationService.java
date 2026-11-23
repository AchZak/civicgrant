package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.ApplicationReferenceSequence;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationReference;
import be.achzak.civicgrant.core.grantapplication.domain.EligibilityPolicy;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateApplicationService implements CreateApplicationUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final OrganisationRepository organisations;
    private final ApplicationReferenceSequence references;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public ApplicationId create(CreateApplicationCommand command) {
        command.actor().requireAnyRole(Role.APPLICANT);
        FundingCall fundingCall = fundingCalls.getById(command.fundingCallId());
        Organisation organisation = organisations.getById(command.organisationId());
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
