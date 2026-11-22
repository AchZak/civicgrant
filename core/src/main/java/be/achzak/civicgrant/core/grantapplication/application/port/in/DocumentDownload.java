package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import java.io.InputStream;
import java.util.Objects;

public record DocumentDownload(SupportingDocument document, InputStream content) {

    public DocumentDownload {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(content, "content");
    }
}
