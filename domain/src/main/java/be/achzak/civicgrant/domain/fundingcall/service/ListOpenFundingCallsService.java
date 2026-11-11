package be.achzak.civicgrant.domain.fundingcall.service;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListOpenFundingCallsService implements ListOpenFundingCallsUseCase {

    private static final Comparator<FundingCall> CLOSING_SOONEST_FIRST = Comparator.comparing(
                    (FundingCall call) -> call.submissionPeriod().closesAt())
            .thenComparing(FundingCall::reference);

    private final FundingCallRepository fundingCalls;
    private final Clock clock;

    public ListOpenFundingCallsService(FundingCallRepository fundingCalls, Clock clock) {
        this.fundingCalls = Objects.requireNonNull(fundingCalls, "fundingCalls");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<FundingCall> list(ListOpenFundingCallsQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return fundingCalls.findOpenAt(clock.instant()).stream()
                .sorted(CLOSING_SOONEST_FIRST)
                .toList();
    }
}
