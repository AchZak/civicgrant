package be.achzak.civicgrant.domain.fundingcall.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import java.util.List;

public interface ListOpenFundingCallsUseCase {

    List<FundingCall> list(ListOpenFundingCallsQuery query);
}
