package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grant_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class GrantApplicationJpaEntity {

    @Id
    private UUID id;

    private String reference;
    private UUID fundingCallId;
    private UUID organisationId;
    private String applicantOid;

    @Setter(AccessLevel.PACKAGE)
    private String projectTitle;

    @Setter(AccessLevel.PACKAGE)
    private String projectDescription;

    @Setter(AccessLevel.PACKAGE)
    private BigDecimal requestedAmount;

    @Setter(AccessLevel.PACKAGE)
    private BigDecimal totalProjectBudget;

    @Setter(AccessLevel.PACKAGE)
    @Enumerated(EnumType.STRING)
    private Language language;

    @Setter(AccessLevel.PACKAGE)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Setter(AccessLevel.PACKAGE)
    private Instant submittedAt;

    @Version
    private long version;

    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    GrantApplicationJpaEntity(
            UUID id,
            String reference,
            UUID fundingCallId,
            UUID organisationId,
            String applicantOid,
            Instant createdAt) {
        this.id = id;
        this.reference = reference;
        this.fundingCallId = fundingCallId;
        this.organisationId = organisationId;
        this.applicantOid = applicantOid;
        this.createdAt = createdAt;
    }
}
