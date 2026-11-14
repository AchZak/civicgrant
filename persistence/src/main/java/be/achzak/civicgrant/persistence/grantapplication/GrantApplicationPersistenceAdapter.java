package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationVisibility;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public ApplicationPage search(
            ApplicationVisibility visibility,
            Set<ApplicationStatus> statuses,
            Set<FundingCallId> fundingCalls,
            int page,
            int pageSize) {
        SearchCriteria criteria = new SearchCriteria(visibility, statuses, fundingCalls);
        long total = count(criteria);
        long offset = (long) (page - 1) * pageSize;
        List<ApplicationSummary> items = offset < total ? load(criteria, (int) offset, pageSize) : List.of();
        return new ApplicationPage(items, page, pageSize, total);
    }

    private long count(SearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<GrantApplicationJpaEntity> application = query.from(GrantApplicationJpaEntity.class);
        query.select(builder.count(application)).where(criteria.predicates(builder, application));
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<ApplicationSummary> load(SearchCriteria criteria, int offset, int pageSize) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<GrantApplicationSummaryRow> query = builder.createQuery(GrantApplicationSummaryRow.class);
        Root<GrantApplicationJpaEntity> application = query.from(GrantApplicationJpaEntity.class);
        query.select(builder.construct(
                        GrantApplicationSummaryRow.class,
                        application.get("id"),
                        application.get("reference"),
                        application.get("fundingCallId"),
                        application.get("organisationId"),
                        application.get("status"),
                        application.get("projectTitle"),
                        application.get("requestedAmount"),
                        application.get("submittedAt"),
                        application.get("updatedAt")))
                .where(criteria.predicates(builder, application))
                .orderBy(builder.desc(application.get("updatedAt")), builder.asc(application.get("id")));
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
                "CONCURRENT_UPDATE",
                "Application " + application.reference().value() + " was changed by someone else since it was read");
    }

    private record SearchCriteria(
            ApplicationVisibility visibility, Set<ApplicationStatus> statuses, Set<FundingCallId> fundingCalls) {

        Predicate[] predicates(CriteriaBuilder builder, Root<GrantApplicationJpaEntity> application) {
            List<Predicate> predicates = new ArrayList<>();
            Predicate own = builder.equal(
                    application.get("applicantOid"), visibility.user().value());
            Predicate leftDraft = builder.notEqual(application.get("status"), ApplicationStatus.DRAFT);
            predicates.add(visibility.staff() ? builder.or(own, leftDraft) : own);
            if (!statuses.isEmpty()) {
                predicates.add(application.get("status").in(statuses));
            }
            if (!fundingCalls.isEmpty()) {
                predicates.add(application
                        .get("fundingCallId")
                        .in(fundingCalls.stream().map(FundingCallId::value).toList()));
            }
            return predicates.toArray(Predicate[]::new);
        }
    }
}
