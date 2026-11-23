package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record DownloadDocumentQuery(ApplicationId applicationId, DocumentId documentId, Actor actor) implements Query {}
