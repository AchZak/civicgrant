package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
class AuditTrailPersistenceAdapter implements AuditTrail {

    private final EntityManager entityManager;

    AuditTrailPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void append(AuditEntry entry) {
        entityManager.persist(AuditEventJpaMapper.toEntity(entry));
    }
}
