package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.shared.Money.euros;

import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;
import java.time.Duration;
import java.time.Instant;

public final class FundingCallBuilder {

    public static final Instant OPENS_AT = Instant.parse("2026-03-01T08:00:00Z");
    public static final Instant CLOSES_AT = Instant.parse("2026-06-30T22:00:00Z");

    private FundingCallId id = FundingCallId.generate();
    private Instant opensAt = OPENS_AT;
    private Instant closesAt = CLOSES_AT;
    private Money totalBudget = euros("50000.00");
    private Money committed = Money.ZERO;
    private Money capPerApplication = euros("15000.00");

    private FundingCallBuilder() {}

    public static FundingCallBuilder aFundingCall() {
        return new FundingCallBuilder();
    }

    public FundingCallBuilder withId(FundingCallId id) {
        this.id = id;
        return this;
    }

    public FundingCallBuilder withSubmissionPeriod(Instant opensAt, Instant closesAt) {
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        return this;
    }

    public FundingCallBuilder withTotalBudget(Money totalBudget) {
        this.totalBudget = totalBudget;
        return this;
    }

    public FundingCallBuilder withCommitted(Money committed) {
        this.committed = committed;
        return this;
    }

    public FundingCallBuilder withCapPerApplication(Money capPerApplication) {
        this.capPerApplication = capPerApplication;
        return this;
    }

    public FundingCall build() {
        return FundingCall.restore(
                id,
                "NBH-2026",
                new LocalizedText("Buurtprojecten 2026", "Projets de quartier 2026"),
                new SubmissionPeriod(opensAt, closesAt),
                new BudgetEnvelope(totalBudget, committed, capPerApplication),
                0,
                opensAt.minus(Duration.ofDays(30)));
    }
}
