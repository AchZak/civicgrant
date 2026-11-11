package be.achzak.civicgrant.domain.fundingcall.port.out;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FundingCallRepository {

    Optional<FundingCall> findById(FundingCallId id);

    List<FundingCall> findOpenAt(Instant moment);
}
