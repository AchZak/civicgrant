package be.achzak.civicgrant.core.fundingcall.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import java.util.List;

public interface ListOpenFundingCallsUseCase {

    List<FundingCall> list(ListOpenFundingCallsQuery query);
}
