package be.achzak.civicgrant.blobstorage;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;

final class DocumentBlobNames {

    private DocumentBlobNames() {}

    static String of(ApplicationId applicationId, DocumentId documentId) {
        return "applications/%s/%s.pdf".formatted(applicationId.value(), documentId.value());
    }
}
