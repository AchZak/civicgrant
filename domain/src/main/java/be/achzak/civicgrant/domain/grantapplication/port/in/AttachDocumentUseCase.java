package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;

public interface AttachDocumentUseCase {

    DocumentId attach(AttachDocumentCommand command);
}
