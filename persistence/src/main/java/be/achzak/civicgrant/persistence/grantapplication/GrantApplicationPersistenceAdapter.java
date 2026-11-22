package be.achzak.civicgrant.persistence.grantapplication;

import static be.achzak.civicgrant.core.sharedkernel.domain.SharedErrorCode.CONCURRENT_UPDATE;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationVisibility;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.domain.error.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public ApplicationPage search(ApplicationSearchCriteria criteria, int page, int pageSize) {
        long total = count(criteria);
        long offset = (long) (page - 1) * pageSize;
        List<ApplicationSummary> items = offset < total ? load(criteria, (int) offset, pageSize) : List.of();
        return new ApplicationPage(items, page, pageSize, total);
    }

    private long count(ApplicationSearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<GrantApplicationJpaEntity> application = query.from(GrantApplicationJpaEntity.class);
        query.select(builder.count(application)).where(predicates(criteria, builder, application));
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<ApplicationSummary> load(ApplicationSearchCriteria criteria, int offset, int pageSize) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<GrantApplicationSummaryRow> query = builder.createQuery(GrantApplicationSummaryRow.class);
        Root<GrantApplicationJpaEntity> application = query.from(GrantApplicationJpaEntity.class);
        query.select(builder.construct(
                        GrantApplicationSummaryRow.class,
                        application.get(GrantApplicationJpaEntity_.id),
                        application.get(GrantApplicationJpaEntity_.reference),
                        application.get(GrantApplicationJpaEntity_.fundingCallId),
                        application.get(GrantApplicationJpaEntity_.organisationId),
                        application.get(GrantApplicationJpaEntity_.status),
                        application.get(GrantApplicationJpaEntity_.projectTitle),
                        application.get(GrantApplicationJpaEntity_.requestedAmount),
                        application.get(GrantApplicationJpaEntity_.submittedAt),
                        application.get(GrantApplicationJpaEntity_.updatedAt)))
                .where(predicates(criteria, builder, application))
                .orderBy(
                        builder.desc(application.get(GrantApplicationJpaEntity_.updatedAt)),
                        builder.asc(application.get(GrantApplicationJpaEntity_.id)));
        return entityManager
                .createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(pageSize)
                .getResultStream()
                .map(GrantApplicationJpaMapper::toSummary)
                .toList();
    }

    private static ConflictException concurrentUpdate(GrantApplication application) {
        return new ConflictException(
                CONCURRENT_UPDATE,
                "Application " + application.reference().value() + " was changed by someone else since it was read");
    }

    private static Predicate[] predicates(
            ApplicationSearchCriteria criteria, CriteriaBuilder builder, Root<GrantApplicationJpaEntity> application) {
        List<Predicate> predicates = new ArrayList<>();
        ApplicationVisibility visibility = criteria.visibility();
        Predicate own = builder.equal(
                application.get(GrantApplicationJpaEntity_.applicantOid),
                visibility.user().value());
        Predicate leftDraft =
                builder.notEqual(application.get(GrantApplicationJpaEntity_.status), ApplicationStatus.DRAFT);
        predicates.add(visibility.staff() ? builder.or(own, leftDraft) : own);
        if (!criteria.statuses().isEmpty()) {
            predicates.add(application.get(GrantApplicationJpaEntity_.status).in(criteria.statuses()));
        }
        if (!criteria.fundingCalls().isEmpty()) {
            predicates.add(application
                    .get(GrantApplicationJpaEntity_.fundingCallId)
                    .in(criteria.fundingCalls().stream()
                            .map(FundingCallId::value)
                            .toList()));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
