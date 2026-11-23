package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.DocumentStorage;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.DomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttachDocumentService implements AttachDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DocumentStorage documents;
    private final DomainEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public DocumentId attach(AttachDocumentCommand command) {
        command.actor().requireAnyRole(Role.APPLICANT);
        GrantApplication application = applications.getById(command.applicationId());
        // The foreign key guarantees the call exists, so a missing one is a bug.
        FundingCall fundingCall =
                fundingCalls.findById(application.fundingCallId()).orElseThrow();
        SupportingDocument document = application.attachDocument(
                command.fileName(),
                command.declaredContentType(),
                command.content(),
                fundingCall,
                command.actor(),
                clock.instant());
        applications.save(application);
        events.publish(application.pullEvents());
        // The file goes last: what the database refuses fails before it exists, and a failed upload undoes the rest.
        documents.store(
                application.id(), document.id(), new ByteArrayInputStream(command.content()), document.sizeInBytes());
        return document.id();
    }
}
