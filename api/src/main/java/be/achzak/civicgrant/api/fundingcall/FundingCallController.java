package be.achzak.civicgrant.api.fundingcall;

import be.achzak.civicgrant.api.generated.api.FundingCallsApi;
import be.achzak.civicgrant.api.generated.model.FundingCall;
import be.achzak.civicgrant.api.generated.model.FundingCallCollection;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FundingCallController implements FundingCallsApi {

    private final ListOpenFundingCallsUseCase listOpenFundingCalls;
    private final GetFundingCallUseCase getFundingCall;
    private final CurrentActor currentActor;

    FundingCallController(
            ListOpenFundingCallsUseCase listOpenFundingCalls,
            GetFundingCallUseCase getFundingCall,
            CurrentActor currentActor) {
        this.listOpenFundingCalls = listOpenFundingCalls;
        this.getFundingCall = getFundingCall;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<FundingCallCollection> listOpenFundingCalls() {
        ListOpenFundingCallsQuery query = new ListOpenFundingCallsQuery(currentActor.get());
        return ResponseEntity.ok(FundingCallApiMapper.toCollection(listOpenFundingCalls.list(query)));
    }

    @Override
    public ResponseEntity<FundingCall> getFundingCall(UUID fundingCallId) {
        GetFundingCallQuery query = new GetFundingCallQuery(new FundingCallId(fundingCallId), currentActor.get());
        return ResponseEntity.ok(FundingCallApiMapper.toResource(getFundingCall.get(query)));
    }
}
