package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.model.AuditSubject;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import java.util.UUID;

final class AuditEventJpaMapper {

    private AuditEventJpaMapper() {}

    static AuditEventJpaEntity toEntity(AuditEntry entry) {
        return switch (entry.subject()) {
            case AuditSubject.Application(ApplicationId id) -> entity(id.value(), null, entry);
            case AuditSubject.Organisation(OrganisationId id) -> entity(null, id.value(), entry);
        };
    }

    private static AuditEventJpaEntity entity(UUID grantApplicationId, UUID organisationId, AuditEntry entry) {
        return new AuditEventJpaEntity(
                grantApplicationId,
                organisationId,
                entry.action(),
                entry.actorId().value(),
                entry.occurredAt(),
                entry.details());
    }
}
