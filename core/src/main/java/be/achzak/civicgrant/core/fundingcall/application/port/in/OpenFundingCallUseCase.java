package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;

public interface OpenFundingCallUseCase {

    FundingCallId open(OpenFundingCallCommand command);
}
