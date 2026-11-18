package be.achzak.civicgrant.persistence.fundingcall;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class FundingCallPersistenceAdapter implements FundingCallRepository {

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
            throw new IllegalStateException(
                    "Funding call " + fundingCall.reference() + " isn't stored: calls are only seeded");
        }
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
                "CONCURRENT_UPDATE",
                "Funding call " + fundingCall.reference() + " was changed by someone else since it was read");
    }
}
