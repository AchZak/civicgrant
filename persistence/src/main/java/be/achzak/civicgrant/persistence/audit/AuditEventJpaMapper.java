package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.model.AuditSubject;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import java.util.UUID;

final class AuditEventJpaMapper {

    private AuditEventJpaMapper() {}

    static AuditEventJpaEntity toEntity(AuditEntry entry) {
        return switch (entry.subject()) {
            case AuditSubject.Application(ApplicationId id) -> entity(id.value(), null, null, entry);
            case AuditSubject.Organisation(OrganisationId id) -> entity(null, id.value(), null, entry);
            case AuditSubject.FundingCall(FundingCallId id) -> entity(null, null, id.value(), entry);
        };
    }

    private static AuditEventJpaEntity entity(
            UUID grantApplicationId, UUID organisationId, UUID fundingCallId, AuditEntry entry) {
        return new AuditEventJpaEntity(
                grantApplicationId,
                organisationId,
                fundingCallId,
                entry.action(),
                entry.actorId().value(),
                entry.occurredAt(),
                entry.details());
    }
}
