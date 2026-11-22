package be.achzak.civicgrant.core.fundingcall.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
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
        return fundingCalls.getById(query.fundingCallId());
    }
}
