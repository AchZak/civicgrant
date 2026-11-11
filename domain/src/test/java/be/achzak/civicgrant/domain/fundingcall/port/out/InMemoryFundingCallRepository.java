package be.achzak.civicgrant.domain.fundingcall.port.out;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryFundingCallRepository implements FundingCallRepository {

    private final Map<FundingCallId, FundingCall> fundingCalls = new LinkedHashMap<>();

    public InMemoryFundingCallRepository(FundingCall... fundingCalls) {
        for (FundingCall fundingCall : fundingCalls) {
            this.fundingCalls.put(fundingCall.id(), fundingCall);
        }
    }

    @Override
    public Optional<FundingCall> findById(FundingCallId id) {
        return Optional.ofNullable(fundingCalls.get(id));
    }

    @Override
    public List<FundingCall> findOpenAt(Instant moment) {
        return fundingCalls.values().stream()
                .filter(fundingCall -> fundingCall.isOpenAt(moment))
                .toList();
    }
}
