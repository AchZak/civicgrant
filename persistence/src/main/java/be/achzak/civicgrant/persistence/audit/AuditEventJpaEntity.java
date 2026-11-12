package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.domain.audit.model.AuditAction;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AuditEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID grantApplicationId;
    private UUID organisationId;

    @Enumerated(EnumType.STRING)
    private AuditAction eventType;

    private String actorOid;
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> details;

    AuditEventJpaEntity(
            UUID grantApplicationId,
            UUID organisationId,
            AuditAction eventType,
            String actorOid,
            Instant occurredAt,
            Map<String, String> details) {
        this.grantApplicationId = grantApplicationId;
        this.organisationId = organisationId;
        this.eventType = eventType;
        this.actorOid = actorOid;
        this.occurredAt = occurredAt;
        this.details = details;
    }
}
