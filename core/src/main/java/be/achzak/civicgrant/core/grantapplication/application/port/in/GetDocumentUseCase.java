package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;

public interface GetDocumentUseCase {

    SupportingDocument get(GetDocumentQuery query);
}
