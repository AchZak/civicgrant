package be.achzak.civicgrant.api.grantapplication;

import static java.time.ZoneOffset.UTC;

import be.achzak.civicgrant.api.generated.model.SupportingDocumentDto;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.shared.Actor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

final class SupportingDocumentApiMapper {

    private SupportingDocumentApiMapper() {}

    // A part without a file name or content type gets the domain's own refusal, not a 500.
    static AttachDocumentCommand toCommand(UUID applicationId, MultipartFile file, Actor actor) {
        return new AttachDocumentCommand(
                new ApplicationId(applicationId),
                Objects.requireNonNullElse(file.getOriginalFilename(), ""),
                Objects.requireNonNullElse(file.getContentType(), ""),
                contentOf(file),
                actor);
    }

    static SupportingDocumentDto toDto(SupportingDocument document) {
        return new SupportingDocumentDto(
                document.id().value(),
                document.fileName(),
                document.sizeInBytes(),
                document.uploadedAt().atOffset(UTC));
    }

    private static byte[] contentOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
