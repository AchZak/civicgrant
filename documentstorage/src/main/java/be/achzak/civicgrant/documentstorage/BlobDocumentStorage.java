package be.achzak.civicgrant.documentstorage;

import be.achzak.civicgrant.core.grantapplication.application.port.out.DocumentStorage;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
class BlobDocumentStorage implements DocumentStorage {

    private final BlobContainerClient documents;

    BlobDocumentStorage(BlobContainerClient documents) {
        this.documents = documents;
    }

    @Override
    public void store(ApplicationId applicationId, DocumentId documentId, InputStream content, long sizeInBytes) {
        BlobParallelUploadOptions upload = new BlobParallelUploadOptions(BinaryData.fromStream(content, sizeInBytes))
                .setHeaders(new BlobHttpHeaders().setContentType(SupportingDocument.CONTENT_TYPE));
        blob(applicationId, documentId).uploadWithResponse(upload, null, Context.NONE);
    }

    @Override
    public InputStream open(ApplicationId applicationId, DocumentId documentId) {
        return blob(applicationId, documentId).openInputStream();
    }

    @Override
    public void delete(ApplicationId applicationId, DocumentId documentId) {
        blob(applicationId, documentId).deleteIfExists();
    }

    private BlobClient blob(ApplicationId applicationId, DocumentId documentId) {
        return documents.getBlobClient(DocumentBlobNames.of(applicationId, documentId));
    }
}
