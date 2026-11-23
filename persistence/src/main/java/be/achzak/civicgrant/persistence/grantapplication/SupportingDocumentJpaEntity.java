package be.achzak.civicgrant.persistence.grantapplication;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supporting_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SupportingDocumentJpaEntity {

    @Id
    private UUID id;

    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private String uploadedByOid;
    private Instant uploadedAt;

    SupportingDocumentJpaEntity(
            UUID id,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String uploadedByOid,
            Instant uploadedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedByOid = uploadedByOid;
        this.uploadedAt = uploadedAt;
    }
}
