package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdfOf;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anotherApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.grantapplication.application.port.in.DocumentDownload;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryDocumentStorage;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Downloading a supporting document")
class DownloadDocumentServiceTest {

    private static final SupportingDocument DOCUMENT = aSupportingDocument();
    private static final byte[] CONTENT = aPdfOf((int) DOCUMENT.sizeInBytes());

    private final GrantApplication draft =
            aDraftApplication().withDocuments(DOCUMENT).build();
    private final InMemoryDocumentStorage documents = new InMemoryDocumentStorage();
    private final DownloadDocumentService service =
            new DownloadDocumentService(new InMemoryGrantApplicationRepository(draft), documents);

    @Test
    @DisplayName("gives the document's details with its stored content")
    void givesTheDocumentsDetailsWithItsStoredContent() {
        // given
        documents.store(draft.id(), DOCUMENT.id(), new ByteArrayInputStream(CONTENT), CONTENT.length);

        // when
        DocumentDownload download =
                service.download(new DownloadDocumentQuery(draft.id(), DOCUMENT.id(), anApplicant()));

        // then
        assertThat(download.document()).isEqualTo(DOCUMENT);
        assertThat(download.content()).hasBinaryContent(CONTENT);
    }

    @Test
    @DisplayName("hides the content from another applicant, as if the application didn't exist")
    void hidesTheContentFromAnotherApplicant() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.download(new DownloadDocumentQuery(draft.id(), DOCUMENT.id(), anotherApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesAnActorWithoutAnyRole() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.download(new DownloadDocumentQuery(draft.id(), DOCUMENT.id(), anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
