package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;

public interface GetDocumentUseCase {

    SupportingDocument get(GetDocumentQuery query);
}
