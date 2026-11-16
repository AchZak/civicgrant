package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.DocumentStorage;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.DomainEventPublisher;
import be.achzak.civicgrant.domain.shared.Role;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.Objects;

public class AttachDocumentService implements AttachDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final FundingCallRepository fundingCalls;
    private final DocumentStorage documents;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AttachDocumentService(
            GrantApplicationRepository applications,
            FundingCallRepository fundingCalls,
            DocumentStorage documents,
            DomainEventPublisher events,
            Clock clock) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
        this.documents = Objects.requireNonNull(documents, "documents");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public DocumentId attach(AttachDocumentCommand command) {
        Objects.requireNonNull(command, "command");
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
