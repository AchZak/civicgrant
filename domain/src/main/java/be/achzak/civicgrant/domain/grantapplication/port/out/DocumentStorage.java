package be.achzak.civicgrant.domain.grantapplication.port.out;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import java.io.InputStream;

public interface DocumentStorage {

    void store(ApplicationId applicationId, DocumentId documentId, InputStream content, long sizeInBytes);

    InputStream open(ApplicationId applicationId, DocumentId documentId);

    // Deleting a file that's already gone does nothing, so a retried cleanup never fails.
    void delete(ApplicationId applicationId, DocumentId documentId);
}
