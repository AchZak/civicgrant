package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;

final class DocumentBlobNames {

    private DocumentBlobNames() {}

    static String of(ApplicationId applicationId, DocumentId documentId) {
        return "applications/%s/%s.pdf".formatted(applicationId.value(), documentId.value());
    }
}
