package be.achzak.civicgrant.core.fundingcall.application.port.out;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.FUNDING_CALL_NOT_FOUND;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.sharedkernel.domain.error.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FundingCallRepository {

    Optional<FundingCall> findById(FundingCallId id);

    default FundingCall getById(FundingCallId id) {
        return findById(id)
                .orElseThrow(() ->
                        new NotFoundException(FUNDING_CALL_NOT_FOUND, "Funding call " + id.value() + " not found"));
    }

    List<FundingCall> findOpenAt(Instant moment);

    void save(FundingCall fundingCall);
}
