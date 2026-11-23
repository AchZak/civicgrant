package be.achzak.civicgrant.core.fundingcall.application.service;

import static be.achzak.civicgrant.core.common.domain.identity.Actors.anApplicant;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.OPENS_AT;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.fundingcall.application.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Listing open funding calls")
class ListOpenFundingCallsServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("lists the calls closing soonest first, then by reference")
    void listsTheCallsClosingSoonestFirstThenByReference() {
        // given
        FundingCall closingLater = aFundingCall()
                .withReference("ARTS-2026")
                .withSubmissionPeriod(OPENS_AT, NOW.plus(Duration.ofDays(60)))
                .build();
        FundingCall sport = aFundingCall()
                .withReference("SPORT-2026")
                .withSubmissionPeriod(OPENS_AT, NOW.plus(Duration.ofDays(10)))
                .build();
        FundingCall culture = aFundingCall()
                .withReference("CULTURE-2026")
                .withSubmissionPeriod(OPENS_AT, NOW.plus(Duration.ofDays(10)))
                .build();
        ListOpenFundingCallsService service =
                new ListOpenFundingCallsService(new InMemoryFundingCallRepository(closingLater, sport, culture), CLOCK);

        // when
        List<FundingCall> calls = service.list(new ListOpenFundingCallsQuery(anApplicant()));

        // then
        assertThat(calls).containsExactly(culture, sport, closingLater);
    }
}
