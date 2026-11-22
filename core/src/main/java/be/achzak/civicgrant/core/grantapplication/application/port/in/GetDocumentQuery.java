package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record GetDocumentQuery(ApplicationId applicationId, DocumentId documentId, Actor actor) implements Query {

    public GetDocumentQuery {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(actor, "actor");
    }
}
