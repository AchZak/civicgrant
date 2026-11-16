package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import java.io.InputStream;
import java.util.Objects;

public record DocumentDownload(SupportingDocument document, InputStream content) {

    public DocumentDownload {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(content, "content");
    }
}
