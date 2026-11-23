package be.achzak.civicgrant.persistence.audit;

import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AuditTrailPersistenceAdapter implements AuditTrail {

    private final EntityManager entityManager;

    @Override
    public void append(AuditEntry entry) {
        entityManager.persist(AuditEventJpaMapper.toEntity(entry));
    }
}
