package be.achzak.civicgrant.api.fundingcall;

import be.achzak.civicgrant.api.generated.api.FundingCallsApi;
import be.achzak.civicgrant.api.generated.model.FundingCallCollectionDto;
import be.achzak.civicgrant.api.generated.model.FundingCallDto;
import be.achzak.civicgrant.api.generated.model.OpenFundingCallRequestDto;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class FundingCallController implements FundingCallsApi {

    private final OpenFundingCallUseCase openFundingCall;
    private final ListOpenFundingCallsUseCase listOpenFundingCalls;
    private final GetFundingCallUseCase getFundingCall;
    private final CurrentActor currentActor;

    FundingCallController(
            OpenFundingCallUseCase openFundingCall,
            ListOpenFundingCallsUseCase listOpenFundingCalls,
            GetFundingCallUseCase getFundingCall,
            CurrentActor currentActor) {
        this.openFundingCall = openFundingCall;
        this.listOpenFundingCalls = listOpenFundingCalls;
        this.getFundingCall = getFundingCall;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<FundingCallDto> openFundingCall(OpenFundingCallRequestDto request) {
        Actor actor = currentActor.get();
        FundingCallId id = openFundingCall.open(FundingCallApiMapper.toCommand(request, actor));
        FundingCallDto opened = FundingCallApiMapper.toDto(getFundingCall.get(new GetFundingCallQuery(id, actor)));
        return ResponseEntity.created(locationOf(id)).body(opened);
    }

    @Override
    public ResponseEntity<FundingCallCollectionDto> listOpenFundingCalls() {
        ListOpenFundingCallsQuery query = new ListOpenFundingCallsQuery(currentActor.get());
        return ResponseEntity.ok(FundingCallApiMapper.toDto(listOpenFundingCalls.list(query)));
    }

    @Override
    public ResponseEntity<FundingCallDto> getFundingCall(UUID fundingCallId) {
        GetFundingCallQuery query = new GetFundingCallQuery(new FundingCallId(fundingCallId), currentActor.get());
        return ResponseEntity.ok(FundingCallApiMapper.toDto(getFundingCall.get(query)));
    }

    private static URI locationOf(FundingCallId id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{fundingCallId}")
                .buildAndExpand(id.value())
                .toUri();
    }
}
