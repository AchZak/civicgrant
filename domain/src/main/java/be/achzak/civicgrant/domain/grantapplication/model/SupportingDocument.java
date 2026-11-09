package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record SupportingDocument(
        DocumentId id, String fileName, long sizeInBytes, UserId uploadedBy, Instant uploadedAt) {

    public static final String CONTENT_TYPE = "application/pdf";
    public static final long MAX_SIZE_IN_BYTES = 10L * 1024 * 1024;
    public static final int MAX_FILE_NAME_LENGTH = 255;

    public SupportingDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }

    static SupportingDocument upload(
            String fileName, String declaredContentType, long sizeInBytes, UserId uploadedBy, Instant uploadedAt) {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(declaredContentType, "declaredContentType");
        String validFileName = validFileName(fileName);
        if (!CONTENT_TYPE.equalsIgnoreCase(declaredContentType.strip())) {
            throw new BusinessRuleViolationException(
                    "DOCUMENT_NOT_PDF", "Only PDF documents are accepted, not " + declaredContentType);
        }
        if (sizeInBytes <= 0) {
            throw new BusinessRuleViolationException("DOCUMENT_EMPTY", "Document " + validFileName + " is empty");
        }
        if (sizeInBytes > MAX_SIZE_IN_BYTES) {
            throw new BusinessRuleViolationException(
                    "DOCUMENT_TOO_LARGE",
                    "Document " + validFileName + " has " + sizeInBytes + " bytes, more than " + MAX_SIZE_IN_BYTES);
        }
        return new SupportingDocument(DocumentId.generate(), validFileName, sizeInBytes, uploadedBy, uploadedAt);
    }

    private static String validFileName(String fileName) {
        String stripped = fileName.strip();
        boolean invalid = stripped.isEmpty()
                || stripped.length() > MAX_FILE_NAME_LENGTH
                || stripped.chars().anyMatch(c -> c == '/' || c == '\\' || Character.isISOControl(c));
        if (invalid) {
            throw new BusinessRuleViolationException(
                    "DOCUMENT_NAME_INVALID",
                    "A file name has 1 to " + MAX_FILE_NAME_LENGTH + " characters, without path or control characters");
        }
        return stripped;
    }
}
