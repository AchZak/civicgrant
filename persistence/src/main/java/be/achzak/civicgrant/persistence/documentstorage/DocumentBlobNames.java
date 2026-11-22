package be.achzak.civicgrant.persistence.documentstorage;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;

// Public because the grant application mapper stores the same name in each document's row.
public final class DocumentBlobNames {

    private DocumentBlobNames() {}

    public static String of(ApplicationId applicationId, DocumentId documentId) {
        return "applications/%s/%s.pdf".formatted(applicationId.value(), documentId.value());
    }
}
