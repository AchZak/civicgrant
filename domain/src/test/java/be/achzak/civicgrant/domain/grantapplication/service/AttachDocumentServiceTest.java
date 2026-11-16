package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.PdfFiles.aPdf;
import static be.achzak.civicgrant.domain.grantapplication.model.PdfFiles.aPng;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentAttached;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryDocumentStorage;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.RecordingDomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attaching a supporting document")
class AttachDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final FundingCall FUNDING_CALL = aFundingCall().build();
    private static final byte[] PDF = aPdf();

    private final GrantApplication draft =
            aDraftApplication().forCall(FUNDING_CALL).withoutDocuments().build();
    private final GrantApplication submitted =
            aSubmittedApplication().forCall(FUNDING_CALL).build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(draft, submitted);
    private final InMemoryDocumentStorage documents = new InMemoryDocumentStorage();
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final AttachDocumentService service = new AttachDocumentService(
            applications, new InMemoryFundingCallRepository(FUNDING_CALL), documents, events, CLOCK);

    private static AttachDocumentCommand attaching(ApplicationId application, byte[] content, Actor actor) {
        return new AttachDocumentCommand(application, "begroting.pdf", "application/pdf", content, actor);
    }

    @Test
    @DisplayName("adds the document to the draft under the id it returns")
    void addsTheDocumentToTheDraftUnderTheIdItReturns() {
        // when
        DocumentId id = service.attach(attaching(draft.id(), PDF, anApplicant()));

        // then
        assertThat(applications.findById(draft.id()).orElseThrow().documents())
                .singleElement()
                .returns(id, SupportingDocument::id)
                .returns("begroting.pdf", SupportingDocument::fileName)
                .returns((long) PDF.length, SupportingDocument::sizeInBytes);
    }

    @Test
    @DisplayName("stores the file under the application and the new document")
    void storesTheFileUnderTheApplicationAndTheNewDocument() {
        // when
        DocumentId id = service.attach(attaching(draft.id(), PDF, anApplicant()));

        // then
        assertThat(documents.open(draft.id(), id)).hasBinaryContent(PDF);
    }

    @Test
    @DisplayName("publishes the attachment, with the actor and the moment")
    void publishesTheAttachment() {
        // when
        DocumentId id = service.attach(attaching(draft.id(), PDF, anApplicant()));

        // then
        assertThat(events.published())
                .containsExactly(
                        new DocumentAttached(draft.id(), id, anApplicant().id(), NOW));
    }

    @Test
    @DisplayName("stores no file and publishes nothing when the content isn't a PDF")
    void storesNoFileAndPublishesNothingWhenTheContentIsNotAPdf() {
        // when
        Throwable thrown = catchThrowable(() -> service.attach(attaching(draft.id(), aPng(), anApplicant())));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        assertThat(documents.isEmpty()).isTrue();
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("stores no file for an application that is no longer a draft")
    void storesNoFileForAnApplicationThatIsNoLongerADraft() {
        // when
        Throwable thrown = catchThrowable(() -> service.attach(attaching(submitted.id(), PDF, anApplicant())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
        assertThat(documents.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.attach(attaching(ApplicationId.generate(), PDF, anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an actor without the applicant role")
    void refusesAnActorWithoutTheApplicantRole() {
        // when
        Throwable thrown = catchThrowable(() -> service.attach(attaching(draft.id(), PDF, anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        assertThat(documents.isEmpty()).isTrue();
    }
}
