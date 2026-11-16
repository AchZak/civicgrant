package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.grantapplication.model.PdfFiles.aPdfOf;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.DocumentDownload;
import be.achzak.civicgrant.domain.grantapplication.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryDocumentStorage;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
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
