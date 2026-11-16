package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record DownloadDocumentQuery(ApplicationId applicationId, DocumentId documentId, Actor actor) {

    public DownloadDocumentQuery {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(actor, "actor");
    }
}
