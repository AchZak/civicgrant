package be.achzak.civicgrant.core.fundingcall.application.service;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.aProgrammeManager;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.actor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.common.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.fundingcall.application.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallOpened;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Opening a funding call")
class OpenFundingCallServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final FundingCallReference REFERENCE = new FundingCallReference("GRN-2027");
    private static final LocalizedText TITLE = new LocalizedText("Groene daken 2027", "Toitures vertes 2027");
    private static final SubmissionPeriod PERIOD =
            new SubmissionPeriod(NOW.plus(Duration.ofDays(14)), NOW.plus(Duration.ofDays(120)));

    private final InMemoryFundingCallRepository fundingCalls = new InMemoryFundingCallRepository();
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final OpenFundingCallService service = new OpenFundingCallService(fundingCalls, events, CLOCK);

    private static OpenFundingCallCommand openingBy(Actor actor) {
        return new OpenFundingCallCommand(REFERENCE, TITLE, PERIOD, euros("80000.00"), euros("20000.00"), actor);
    }

    @Test
    @DisplayName("saves the new call and returns its id")
    void savesTheNewCallAndReturnsItsId() {
        // when
        FundingCallId id = service.open(openingBy(aProgrammeManager()));

        // then
        assertThat(fundingCalls.findById(id))
                .get()
                .extracting(
                        FundingCall::reference,
                        FundingCall::title,
                        FundingCall::submissionPeriod,
                        FundingCall::createdAt)
                .containsExactly(REFERENCE, TITLE, PERIOD, NOW);
    }

    @Test
    @DisplayName("publishes the opening, with the actor and the moment")
    void publishesTheOpening() {
        // when
        FundingCallId id = service.open(openingBy(aProgrammeManager()));

        // then
        assertThat(events.published())
                .containsExactly(new FundingCallOpened(
                        id, REFERENCE, euros("80000.00"), aProgrammeManager().id(), NOW));
    }

    @Test
    @DisplayName("saves and publishes nothing when a rule refuses the call")
    void savesAndPublishesNothingWhenARuleRefusesTheCall() {
        // given
        OpenFundingCallCommand alreadyClosed = new OpenFundingCallCommand(
                REFERENCE,
                TITLE,
                new SubmissionPeriod(NOW.minus(Duration.ofDays(30)), NOW),
                euros("80000.00"),
                euros("20000.00"),
                aProgrammeManager());

        // when
        Throwable thrown = catchThrowable(() -> service.open(alreadyClosed));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_ALREADY_CLOSED");
        assertThat(fundingCalls.findOpenAt(PERIOD.opensAt())).isEmpty();
        assertThat(events.published()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Role.class, names = "PROGRAMME_MANAGER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("refuses every role but programme manager, saving and publishing nothing")
    void refusesEveryRoleButProgrammeManager(Role role) {
        // when
        Throwable thrown = catchThrowable(() -> service.open(openingBy(actor("user-1", role))));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        assertThat(fundingCalls.findOpenAt(PERIOD.opensAt())).isEmpty();
        assertThat(events.published()).isEmpty();
    }
}
