package be.achzak.civicgrant.domain.fundingcall.port.out;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_NOT_FOUND;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
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
