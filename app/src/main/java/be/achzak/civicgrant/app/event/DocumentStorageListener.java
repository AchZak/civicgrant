package be.achzak.civicgrant.app.event;

import be.achzak.civicgrant.core.grantapplication.application.port.out.DocumentStorage;
import be.achzak.civicgrant.core.grantapplication.domain.event.DocumentRemoved;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class DocumentStorageListener {

    private final DocumentStorage documents;

    DocumentStorageListener(DocumentStorage documents) {
        this.documents = documents;
    }

    // Only once the removal is committed, so a rolled-back removal keeps its file.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(DocumentRemoved event) {
        documents.delete(event.applicationId(), event.documentId());
    }
}
