package be.achzak.civicgrant.core.grantapplication.application.port.out;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryDocumentStorage implements DocumentStorage {

    private final Map<FileKey, byte[]> files = new HashMap<>();

    @Override
    public void store(ApplicationId applicationId, DocumentId documentId, InputStream content, long sizeInBytes) {
        try {
            byte[] bytes = content.readAllBytes();
            if (bytes.length != sizeInBytes) {
                throw new IllegalArgumentException("Expected " + sizeInBytes + " bytes, got " + bytes.length);
            }
            files.put(new FileKey(applicationId, documentId), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream open(ApplicationId applicationId, DocumentId documentId) {
        byte[] bytes = files.get(new FileKey(applicationId, documentId));
        if (bytes == null) {
            throw new IllegalArgumentException("No file for document " + documentId.value());
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void delete(ApplicationId applicationId, DocumentId documentId) {
        files.remove(new FileKey(applicationId, documentId));
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    private record FileKey(ApplicationId applicationId, DocumentId documentId) {}
}
