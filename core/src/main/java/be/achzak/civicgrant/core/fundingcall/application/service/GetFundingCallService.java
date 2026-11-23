package be.achzak.civicgrant.core.fundingcall.application.service;

import be.achzak.civicgrant.core.fundingcall.application.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFundingCallService implements GetFundingCallUseCase {

    private final FundingCallRepository fundingCalls;

    public GetFundingCallService(FundingCallRepository fundingCalls) {
        this.fundingCalls = fundingCalls;
    }

    @Override
    @Transactional(readOnly = true)
    public FundingCall get(GetFundingCallQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER, Role.PROGRAMME_MANAGER);
        return fundingCalls.getById(query.fundingCallId());
    }
}
