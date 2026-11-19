package be.achzak.civicgrant.persistence.fundingcall;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.BudgetEnvelope;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Funding call persistence")
class FundingCallPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    @Autowired
    private FundingCallRepository fundingCalls;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("loads a funding call with every column")
    void loadsAFundingCallWithEveryColumn() {
        // given
        FundingCall stored = fundingCall("NBH-2026", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofDays(60)));
        insert(stored);

        // when
        Optional<FundingCall> loaded = fundingCalls.findById(stored.id());

        // then
        assertThat(loaded).get().usingRecursiveComparison().isEqualTo(stored);
    }

    @Test
    @DisplayName("finds nothing for an unknown id")
    void findsNothingForAnUnknownId() {
        // when
        Optional<FundingCall> loaded = fundingCalls.findById(FundingCallId.generate());

        // then
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("finds the calls open from their opening instant until just before their closing one")
    void findsTheCallsOpenFromTheirOpeningInstantUntilJustBeforeTheirClosingOne() {
        // given
        FundingCall openingNow = fundingCall("OPENING", NOW, NOW.plus(Duration.ofDays(10)));
        FundingCall closingNow = fundingCall("CLOSING", NOW.minus(Duration.ofDays(30)), NOW);
        FundingCall upcoming = fundingCall("UPCOMING", NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(30)));
        insert(openingNow);
        insert(closingNow);
        insert(upcoming);

        // when
        List<FundingCall> open = fundingCalls.findOpenAt(NOW);

        // then
        assertThat(open).extracting(FundingCall::id).containsExactly(openingNow.id());
    }

    @Nested
    @DisplayName("Saving the committed budget")
    class SavingTheCommittedBudget {

        private static final Actor DECISION_MAKER =
                new Actor(new UserId("decision-maker-1"), Set.of(Role.DECISION_MAKER));

        private final FundingCall stored =
                fundingCall("NBH-SAVE", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofDays(60)));

        @Test
        @DisplayName("saves the committed amount and increments the version")
        void savesTheCommittedAmountAndIncrementsTheVersion() {
            // given
            insert(stored);
            FundingCall loaded = fundingCalls.findById(stored.id()).orElseThrow();
            loaded.commitGrant(euros("7000.00"), DECISION_MAKER, NOW);

            // when
            fundingCalls.save(loaded);

            // then
            entityManager.clear();
            assertThat(fundingCalls.findById(stored.id()))
                    .get()
                    .extracting(call -> call.budget().committed(), FundingCall::version)
                    .containsExactly(euros("19500.00"), 4L);
        }

        @Test
        @DisplayName("refuses a call read before its last change was saved")
        void refusesACallReadBeforeItsLastChangeWasSaved() {
            // given
            insert(stored);
            FundingCall first = fundingCalls.findById(stored.id()).orElseThrow();
            FundingCall second = fundingCalls.findById(stored.id()).orElseThrow();
            first.commitGrant(euros("7000.00"), DECISION_MAKER, NOW);
            fundingCalls.save(first);
            second.commitGrant(euros("9000.00"), DECISION_MAKER, NOW);

            // when
            Throwable thrown = catchThrowable(() -> fundingCalls.save(second));

            // then
            assertThat(thrown)
                    .isInstanceOfSatisfying(
                            ConflictException.class,
                            conflict -> assertThat(conflict.code()).isEqualTo("CONCURRENT_UPDATE"));
        }

        @Test
        @DisplayName("refuses to overwrite a change someone else saved at the same time")
        void refusesToOverwriteAChangeSomeoneElseSavedAtTheSameTime() {
            // given
            insert(stored);
            FundingCall loaded = fundingCalls.findById(stored.id()).orElseThrow();
            jdbc.sql("""
                            UPDATE civicgrant.funding_call
                            SET committed_amount = committed_amount + 9000.00, version = version + 1
                            WHERE id = :id
                            """).param("id", stored.id().value()).update();
            loaded.commitGrant(euros("7000.00"), DECISION_MAKER, NOW);

            // when
            Throwable thrown = catchThrowable(() -> fundingCalls.save(loaded));

            // then
            assertThat(thrown)
                    .isInstanceOfSatisfying(
                            ConflictException.class,
                            conflict -> assertThat(conflict.code()).isEqualTo("CONCURRENT_UPDATE"));
        }
    }

    @Nested
    @DisplayName("Storing a new call")
    class StoringANewCall {

        private final FundingCall opened = newFundingCall("NBH-NEW");

        @Test
        @DisplayName("stores every column of a call that was never stored")
        void storesEveryColumnOfACallThatWasNeverStored() {
            // when
            fundingCalls.save(opened);

            // then
            entityManager.clear();
            assertThat(fundingCalls.findById(opened.id()))
                    .get()
                    .usingRecursiveComparison()
                    .isEqualTo(opened);
        }

        @Test
        @DisplayName("refuses a second call with a reference that's already taken")
        void refusesASecondCallWithAReferenceThatsAlreadyTaken() {
            // given
            insert(fundingCall("NBH-NEW", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofDays(60))));

            // when
            Throwable thrown = catchThrowable(() -> fundingCalls.save(opened));

            // then
            assertThat(thrown)
                    .isInstanceOfSatisfying(
                            ConflictException.class,
                            conflict -> assertThat(conflict.code()).isEqualTo("FUNDING_CALL_REFERENCE_TAKEN"));
        }
    }

    private static FundingCall fundingCall(String reference, Instant opensAt, Instant closesAt) {
        return FundingCall.restore(
                FundingCallId.generate(),
                reference,
                new LocalizedText("Buurtprojecten 2026", "Projets de quartier 2026"),
                new SubmissionPeriod(opensAt, closesAt),
                new BudgetEnvelope(euros("50000.00"), euros("12500.00"), euros("15000.00")),
                3,
                Instant.parse("2026-01-10T09:00:00Z"));
    }

    private static FundingCall newFundingCall(String reference) {
        return FundingCall.restore(
                FundingCallId.generate(),
                reference,
                new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                new SubmissionPeriod(NOW.plus(Duration.ofDays(14)), NOW.plus(Duration.ofDays(120))),
                new BudgetEnvelope(euros("80000.00"), euros("0.00"), euros("20000.00")),
                0,
                NOW);
    }

    private void insert(FundingCall fundingCall) {
        jdbc.sql("""
                        INSERT INTO civicgrant.funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                            total_budget, committed_amount, max_amount_per_application, version, created_at)
                        VALUES (:id, :reference, :titleNl, :titleFr, :opensAt, :closesAt,
                            :totalBudget, :committedAmount, :maxAmountPerApplication, :version, :createdAt)
                        """)
                .param("id", fundingCall.id().value())
                .param("reference", fundingCall.reference())
                .param("titleNl", fundingCall.title().nl())
                .param("titleFr", fundingCall.title().fr())
                .param("opensAt", utc(fundingCall.submissionPeriod().opensAt()))
                .param("closesAt", utc(fundingCall.submissionPeriod().closesAt()))
                .param("totalBudget", fundingCall.budget().total().amount())
                .param("committedAmount", fundingCall.budget().committed().amount())
                .param(
                        "maxAmountPerApplication",
                        fundingCall.budget().capPerApplication().amount())
                .param("version", fundingCall.version())
                .param("createdAt", utc(fundingCall.createdAt()))
                .update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
