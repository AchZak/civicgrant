package be.achzak.civicgrant.persistence.organisation;

import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "organisation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrganisationJpaEntity {

    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String enterpriseNumber;

    private String name;

    @Enumerated(EnumType.STRING)
    private LegalForm legalForm;

    private Instant createdAt;

    OrganisationJpaEntity(UUID id, String enterpriseNumber, String name, LegalForm legalForm, Instant createdAt) {
        this.id = id;
        this.enterpriseNumber = enterpriseNumber;
        this.name = name;
        this.legalForm = legalForm;
        this.createdAt = createdAt;
    }
}
