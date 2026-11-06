package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FundingCall {

    private final FundingCallId id;
    private final String reference;
    private final LocalizedText title;
    private final SubmissionPeriod submissionPeriod;
    private BudgetEnvelope budget;
    private final long version;
    private final Instant createdAt;
    private final List<FundingCallEvent> events = new ArrayList<>();

    private FundingCall(
            FundingCallId id,
            String reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            BudgetEnvelope budget,
            long version,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.title = Objects.requireNonNull(title, "title");
        this.submissionPeriod = Objects.requireNonNull(submissionPeriod, "submissionPeriod");
        this.budget = Objects.requireNonNull(budget, "budget");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static FundingCall restore(
            FundingCallId id,
            String reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            BudgetEnvelope budget,
            long version,
            Instant createdAt) {
        return new FundingCall(id, reference, title, submissionPeriod, budget, version, createdAt);
    }

    public void commitGrant(Money amount, Actor decidedBy, Instant now) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(now, "now");
        if (!amount.isPositive()) {
            throw new BusinessRuleViolationException(
                    "GRANTED_AMOUNT_NOT_POSITIVE", "A granted amount is greater than zero");
        }
        if (amount.isGreaterThan(budget.remaining())) {
            throw new BusinessRuleViolationException(
                    "GRANT_EXCEEDS_REMAINING_BUDGET",
                    "Grant of " + amount.amount() + " exceeds the remaining budget of "
                            + budget.remaining().amount() + " in call " + reference);
        }
        budget = budget.commit(amount);
        events.add(new GrantCommitted(id, amount, decidedBy.id(), now));
    }

    public boolean isOpenAt(Instant moment) {
        return submissionPeriod.isOpenAt(moment);
    }

    public Money remainingBudget() {
        return budget.remaining();
    }

    public Money capPerApplication() {
        return budget.capPerApplication();
    }

    public List<FundingCallEvent> pullEvents() {
        List<FundingCallEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public FundingCallId id() {
        return id;
    }

    public String reference() {
        return reference;
    }

    public LocalizedText title() {
        return title;
    }

    public SubmissionPeriod submissionPeriod() {
        return submissionPeriod;
    }

    public BudgetEnvelope budget() {
        return budget;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
