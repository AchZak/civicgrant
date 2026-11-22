package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;

public interface AttachDocumentUseCase {

    DocumentId attach(AttachDocumentCommand command);
}
