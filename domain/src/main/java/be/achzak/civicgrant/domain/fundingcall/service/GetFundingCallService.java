package be.achzak.civicgrant.domain.fundingcall.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_NOT_FOUND;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
import java.util.Objects;

public class GetFundingCallService implements GetFundingCallUseCase {

    private final FundingCallRepository fundingCalls;

    public GetFundingCallService(FundingCallRepository fundingCalls) {
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
    }

    @Override
    public FundingCall get(GetFundingCallQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER, Role.PROGRAMME_MANAGER);
        return fundingCalls
                .findById(query.fundingCallId())
                .orElseThrow(() -> new NotFoundException(
                        FUNDING_CALL_NOT_FOUND,
                        "Funding call " + query.fundingCallId().value() + " not found"));
    }
}
