package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Query;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;

public record GetDocumentQuery(ApplicationId applicationId, DocumentId documentId, Actor actor) implements Query {}
