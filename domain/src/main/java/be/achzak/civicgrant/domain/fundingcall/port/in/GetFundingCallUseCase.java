package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;

public interface GetFundingCallUseCase {

    FundingCall get(GetFundingCallQuery query);
}
