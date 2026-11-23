package be.achzak.civicgrant.blobstorage;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;

final class DocumentBlobPathResolver {

    private DocumentBlobPathResolver() {}

    static String resolve(ApplicationId applicationId, DocumentId documentId) {
        return "applications/%s/%s.pdf".formatted(applicationId.value(), documentId.value());
    }
}
