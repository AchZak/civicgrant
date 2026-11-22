package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;

public interface GetFundingCallUseCase {

    FundingCall get(GetFundingCallQuery query);
}
