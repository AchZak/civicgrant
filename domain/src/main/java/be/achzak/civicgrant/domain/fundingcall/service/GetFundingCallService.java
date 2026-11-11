package be.achzak.civicgrant.domain.fundingcall.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.Objects;

public class GetFundingCallService implements GetFundingCallUseCase {

    private final FundingCallRepository fundingCalls;

    public GetFundingCallService(FundingCallRepository fundingCalls) {
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
    }

    @Override
    public FundingCall get(GetFundingCallQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return fundingCalls
                .findById(query.fundingCallId())
                .orElseThrow(() -> new NotFoundException(
                        "FUNDING_CALL_NOT_FOUND",
                        "Funding call " + query.fundingCallId().value() + " not found"));
    }
}
