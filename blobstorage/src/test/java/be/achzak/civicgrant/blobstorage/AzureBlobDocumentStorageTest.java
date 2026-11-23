package be.achzak.civicgrant.blobstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.grantapplication.application.port.out.DocumentStorage;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Document storage in Azure Blob Storage")
class AzureBlobDocumentStorageTest {

    private static final byte[] PDF =
            "%PDF-1.7\nbegroting van het buurtproject\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    private final ApplicationId applicationId = ApplicationId.generate();
    private final DocumentId documentId = DocumentId.generate();

    @Autowired
    private DocumentStorage documents;

    @Autowired
    private BlobContainerClient container;

    @Test
    @DisplayName("gives back exactly the bytes it stored")
    void givesBackExactlyTheBytesItStored() throws IOException {
        // given
        documents.store(applicationId, documentId, new ByteArrayInputStream(PDF), PDF.length);

        // when
        byte[] read;
        try (InputStream content = documents.open(applicationId, documentId)) {
            read = content.readAllBytes();
        }

        // then
        assertThat(read).isEqualTo(PDF);
    }

    @Test
    @DisplayName("stores the file under a name made of ids only, as a PDF")
    void storesTheFileUnderANameMadeOfIdsOnlyAsAPdf() {
        // when
        documents.store(applicationId, documentId, new ByteArrayInputStream(PDF), PDF.length);

        // then
        BlobClient blob =
                container.getBlobClient("applications/" + applicationId.value() + "/" + documentId.value() + ".pdf");
        assertThat(blob.exists()).isTrue();
        assertThat(blob.getProperties().getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("deletes a stored file")
    void deletesAStoredFile() {
        // given
        documents.store(applicationId, documentId, new ByteArrayInputStream(PDF), PDF.length);

        // when
        documents.delete(applicationId, documentId);

        // then
        assertThat(container
                        .getBlobClient(DocumentBlobPathResolver.resolve(applicationId, documentId))
                        .exists())
                .isFalse();
    }

    @Test
    @DisplayName("does nothing when asked to delete a file that's already gone")
    void doesNothingWhenAskedToDeleteAFileThatIsAlreadyGone() {
        // when
        Throwable thrown = catchThrowable(() -> documents.delete(applicationId, documentId));

        // then
        assertThat(thrown).isNull();
    }
}
