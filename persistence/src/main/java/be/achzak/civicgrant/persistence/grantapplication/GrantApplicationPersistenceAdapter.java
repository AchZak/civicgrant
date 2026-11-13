package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class GrantApplicationPersistenceAdapter implements GrantApplicationRepository {

    private final EntityManager entityManager;

    GrantApplicationPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<GrantApplication> findById(ApplicationId id) {
        return Optional.ofNullable(entityManager.find(GrantApplicationJpaEntity.class, id.value()))
                .map(GrantApplicationJpaMapper::toDomain);
    }

    @Override
    public void save(GrantApplication application) {
        GrantApplicationJpaEntity stored = entityManager.find(
                GrantApplicationJpaEntity.class, application.id().value());
        if (stored == null) {
            entityManager.persist(GrantApplicationJpaMapper.toEntity(application));
        } else if (stored.getVersion() == application.version()) {
            GrantApplicationJpaMapper.update(stored, application);
        } else {
            throw concurrentUpdate(application);
        }
        try {
            // Flushed now, so an update that lost a race fails here, where it can become a conflict.
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw concurrentUpdate(application);
        }
    }

    private static ConflictException concurrentUpdate(GrantApplication application) {
        return new ConflictException(
                "CONCURRENT_UPDATE",
                "Application " + application.reference().value() + " was changed by someone else since it was read");
    }
}
