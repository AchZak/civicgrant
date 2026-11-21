package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;

public interface OpenFundingCallUseCase {

    FundingCallId open(OpenFundingCallCommand command);
}
