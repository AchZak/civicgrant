package be.achzak.civicgrant.persistence.fundingcall;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.fundingcall.model.BudgetEnvelope;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.fundingcall.port.out.FundingCallRepository;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Funding call persistence")
class FundingCallPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    @Autowired
    private FundingCallRepository fundingCalls;

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
