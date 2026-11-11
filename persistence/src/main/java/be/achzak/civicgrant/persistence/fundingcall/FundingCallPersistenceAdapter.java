package be.achzak.civicgrant.persistence.fundingcall;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class FundingCallPersistenceAdapter implements FundingCallRepository {

    private final FundingCallJpaRepository fundingCalls;

    FundingCallPersistenceAdapter(FundingCallJpaRepository fundingCalls) {
        this.fundingCalls = fundingCalls;
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
}
