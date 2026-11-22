package be.achzak.civicgrant.persistence.fundingcall;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.FUNDING_CALL_REFERENCE_TAKEN;
import static be.achzak.civicgrant.core.sharedkernel.domain.SharedErrorCode.CONCURRENT_UPDATE;

import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.domain.error.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
class FundingCallPersistenceAdapter implements FundingCallRepository {

    private static final String UNIQUE_REFERENCE = "uq_funding_call_reference";

    private final FundingCallJpaRepository fundingCalls;
    private final EntityManager entityManager;

    FundingCallPersistenceAdapter(FundingCallJpaRepository fundingCalls, EntityManager entityManager) {
        this.fundingCalls = fundingCalls;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<FundingCall> findById(FundingCallId id) {
        return fundingCalls.findById(id.value()).map(FundingCallJpaMapper::toDomain);
    }

    @Override
    public List<FundingCall> findOpenAt(Instant moment) {
        return fundingCalls.findOpenAt(moment).stream()
                .map(FundingCallJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void save(FundingCall fundingCall) {
        FundingCallJpaEntity stored =
                entityManager.find(FundingCallJpaEntity.class, fundingCall.id().value());
        if (stored == null) {
            insert(fundingCall);
        } else {
            update(stored, fundingCall);
        }
    }

    private void insert(FundingCall fundingCall) {
        try {
            entityManager.persist(FundingCallJpaMapper.toEntity(fundingCall));
            // Flushed now, so a reference taken concurrently fails here, where it can become a conflict.
            entityManager.flush();
        } catch (ConstraintViolationException e) {
            if (!UNIQUE_REFERENCE.equals(e.getConstraintName())) {
                throw e;
            }
            throw new ConflictException(
                    FUNDING_CALL_REFERENCE_TAKEN,
                    "Funding call reference " + fundingCall.reference().value() + " is already taken");
        }
    }

    private void update(FundingCallJpaEntity stored, FundingCall fundingCall) {
        if (stored.getVersion() != fundingCall.version()) {
            throw concurrentUpdate(fundingCall);
        }
        FundingCallJpaMapper.update(stored, fundingCall);
        try {
            // Flushed now, so an update that lost a race fails here, where it can become a conflict.
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw concurrentUpdate(fundingCall);
        }
    }

    private static ConflictException concurrentUpdate(FundingCall fundingCall) {
        return new ConflictException(
                CONCURRENT_UPDATE,
                "Funding call " + fundingCall.reference().value() + " was changed by someone else since it was read");
    }
}
