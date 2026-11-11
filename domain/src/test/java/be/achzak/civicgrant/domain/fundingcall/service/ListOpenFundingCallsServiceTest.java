package be.achzak.civicgrant.domain.fundingcall.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.OPENS_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Listing open funding calls")
class ListOpenFundingCallsServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("lists only the calls open now")
    void listsOnlyTheCallsOpenNow() {
        // given
        FundingCall open = aFundingCall().build();
        FundingCall upcoming = aFundingCall()
                .withSubmissionPeriod(NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(30)))
                .build();
        FundingCall closingNow =
                aFundingCall().withSubmissionPeriod(OPENS_AT, NOW).build();
        ListOpenFundingCallsService service =
                new ListOpenFundingCallsService(new InMemoryFundingCallRepository(upcoming, open, closingNow), CLOCK);

        // when
        List<FundingCall> calls = service.list(new ListOpenFundingCallsQuery(anApplicant()));

        // then
        assertThat(calls).containsExactly(open);
    }

    @Test
    @DisplayName("lists the calls closing soonest first")
    void listsTheCallsClosingSoonestFirst() {
        // given
        FundingCall closingLater = aFundingCall()
                .withSubmissionPeriod(OPENS_AT, NOW.plus(Duration.ofDays(60)))
                .build();
        FundingCall closingSooner = aFundingCall()
                .withSubmissionPeriod(OPENS_AT, NOW.plus(Duration.ofDays(10)))
                .build();
        ListOpenFundingCallsService service =
                new ListOpenFundingCallsService(new InMemoryFundingCallRepository(closingLater, closingSooner), CLOCK);

        // when
        List<FundingCall> calls = service.list(new ListOpenFundingCallsQuery(anApplicant()));

        // then
        assertThat(calls).containsExactly(closingSooner, closingLater);
    }

    @Test
    @DisplayName("lists calls closing at the same time by reference")
    void listsCallsClosingAtTheSameTimeByReference() {
        // given
        Instant closesAt = NOW.plus(Duration.ofDays(10));
        FundingCall sport = aFundingCall()
                .withReference("SPORT-2026")
                .withSubmissionPeriod(OPENS_AT, closesAt)
                .build();
        FundingCall culture = aFundingCall()
                .withReference("CULTURE-2026")
                .withSubmissionPeriod(OPENS_AT, closesAt)
                .build();
        ListOpenFundingCallsService service =
                new ListOpenFundingCallsService(new InMemoryFundingCallRepository(sport, culture), CLOCK);

        // when
        List<FundingCall> calls = service.list(new ListOpenFundingCallsQuery(anApplicant()));

        // then
        assertThat(calls).containsExactly(culture, sport);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @DisplayName("lets every role list the open calls")
    void letsEveryRoleListTheOpenCalls(Role role) {
        // given
        FundingCall open = aFundingCall().build();
        ListOpenFundingCallsService service =
                new ListOpenFundingCallsService(new InMemoryFundingCallRepository(open), CLOCK);

        // when
        List<FundingCall> calls = service.list(new ListOpenFundingCallsQuery(actor("user-1", role)));

        // then
        assertThat(calls).containsExactly(open);
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesActorWithoutAnyRole() {
        // given
        ListOpenFundingCallsService service = new ListOpenFundingCallsService(
                new InMemoryFundingCallRepository(aFundingCall().build()), CLOCK);

        // when
        Throwable thrown = catchThrowable(() -> service.list(new ListOpenFundingCallsQuery(anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
