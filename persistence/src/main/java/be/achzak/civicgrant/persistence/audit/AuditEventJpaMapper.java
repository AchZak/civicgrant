package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.audit.domain.AuditSubject;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
            @Nullable UUID grantApplicationId,
            @Nullable UUID organisationId,
            @Nullable UUID fundingCallId,
            AuditEntry entry) {
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
