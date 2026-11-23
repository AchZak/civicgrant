package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_EMPTY;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_NAME_INVALID;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_NOT_PDF;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_TOO_LARGE;

import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import be.achzak.civicgrant.core.sharedkernel.domain.error.BusinessRuleViolationException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public record SupportingDocument(
        DocumentId id, String fileName, long sizeInBytes, UserId uploadedBy, Instant uploadedAt) {

    public static final String CONTENT_TYPE = "application/pdf";
    public static final long MAX_SIZE_IN_BYTES = 10L * 1024 * 1024;
    public static final int MAX_FILE_NAME_LENGTH = 255;

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public SupportingDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }

    static SupportingDocument upload(
            String fileName, String declaredContentType, byte[] content, UserId uploadedBy, Instant uploadedAt) {
        String validFileName = validFileName(fileName);
        if (!CONTENT_TYPE.equalsIgnoreCase(declaredContentType.strip())) {
            throw new BusinessRuleViolationException(
                    DOCUMENT_NOT_PDF, "Only PDF documents are accepted, not " + declaredContentType);
        }
        if (content.length == 0) {
            throw new BusinessRuleViolationException(DOCUMENT_EMPTY, "Document " + validFileName + " is empty");
        }
        if (content.length > MAX_SIZE_IN_BYTES) {
            throw new BusinessRuleViolationException(
                    DOCUMENT_TOO_LARGE,
                    "Document " + validFileName + " has " + content.length + " bytes, more than " + MAX_SIZE_IN_BYTES);
        }
        if (!startsWithPdfSignature(content)) {
            throw new BusinessRuleViolationException(
                    DOCUMENT_NOT_PDF, "Document " + validFileName + " doesn't start with the PDF signature %PDF-");
        }
        return new SupportingDocument(DocumentId.generate(), validFileName, content.length, uploadedBy, uploadedAt);
    }

    private static boolean startsWithPdfSignature(byte[] content) {
        return content.length >= PDF_SIGNATURE.length
                && Arrays.equals(content, 0, PDF_SIGNATURE.length, PDF_SIGNATURE, 0, PDF_SIGNATURE.length);
    }

    private static String validFileName(String fileName) {
        String stripped = fileName.strip();
        boolean invalid = stripped.isEmpty()
                || stripped.length() > MAX_FILE_NAME_LENGTH
                || stripped.chars().anyMatch(c -> c == '/' || c == '\\' || Character.isISOControl(c));
        if (invalid) {
            throw new BusinessRuleViolationException(
                    DOCUMENT_NAME_INVALID,
                    "A file name has 1 to " + MAX_FILE_NAME_LENGTH + " characters, without path or control characters");
        }
        return stripped;
    }
}
