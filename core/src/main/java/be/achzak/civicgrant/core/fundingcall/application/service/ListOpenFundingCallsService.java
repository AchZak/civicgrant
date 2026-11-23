package be.achzak.civicgrant.core.fundingcall.application.service;

import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.fundingcall.application.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.in.ListOpenFundingCallsUseCase;
import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListOpenFundingCallsService implements ListOpenFundingCallsUseCase {

    private static final Comparator<FundingCall> CLOSING_SOONEST_FIRST = Comparator.comparing(
                    (FundingCall call) -> call.submissionPeriod().closesAt())
            .thenComparing(call -> call.reference().value());

    private final FundingCallRepository fundingCalls;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<FundingCall> list(ListOpenFundingCallsQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER, Role.PROGRAMME_MANAGER);
        return fundingCalls.findOpenAt(clock.instant()).stream()
                .sorted(CLOSING_SOONEST_FIRST)
                .toList();
    }
}
