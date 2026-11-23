package be.achzak.civicgrant.core.fundingcall.domain;

import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.GRANTED_AMOUNT_NOT_POSITIVE;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.FUNDING_CALL_ALREADY_CLOSED;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.FUNDING_CALL_TITLE_TOO_LONG;
import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.GRANT_EXCEEDS_REMAINING_BUDGET;

import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallEvent;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallOpened;
import be.achzak.civicgrant.core.fundingcall.domain.event.GrantCommitted;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class FundingCall {

    private static final int MAX_TITLE_LENGTH = 200;

    private final FundingCallId id;
    private final FundingCallReference reference;
    private final LocalizedText title;
    private final SubmissionPeriod submissionPeriod;
    private BudgetEnvelope budget;
    private final long version;
    private final Instant createdAt;
    private final List<FundingCallEvent> events = new ArrayList<>();

    private FundingCall(
            FundingCallId id,
            FundingCallReference reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            BudgetEnvelope budget,
            long version,
            Instant createdAt) {
        this.id = id;
        this.reference = reference;
        this.title = title;
        this.submissionPeriod = submissionPeriod;
        this.budget = budget;
        this.version = version;
        this.createdAt = createdAt;
    }

    public static FundingCall create(
            FundingCallReference reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            Money totalBudget,
            Money capPerApplication,
            Actor openedBy,
            Instant now) {
        if (submissionPeriod.isClosedAt(now)) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_ALREADY_CLOSED, "A new funding call closes in the future");
        }
        BudgetEnvelope budget = BudgetEnvelope.unspent(totalBudget, capPerApplication);
        FundingCall fundingCall = new FundingCall(
                FundingCallId.generate(), reference, validTitle(title), submissionPeriod, budget, 0, now);
        fundingCall.events.add(
                new FundingCallOpened(fundingCall.id, fundingCall.reference, budget.total(), openedBy.id(), now));
        return fundingCall;
    }

    private static LocalizedText validTitle(LocalizedText title) {
        LocalizedText stripped =
                new LocalizedText(title.nl().strip(), title.fr().strip());
        if (stripped.nl().length() > MAX_TITLE_LENGTH || stripped.fr().length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_TITLE_TOO_LONG, "A funding call title exceeds " + MAX_TITLE_LENGTH + " characters");
        }
        return stripped;
    }

    public static FundingCall restore(
            FundingCallId id,
            FundingCallReference reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            BudgetEnvelope budget,
            long version,
            Instant createdAt) {
        return new FundingCall(id, reference, title, submissionPeriod, budget, version, createdAt);
    }

    public void commitGrant(Money amount, Actor decidedBy, Instant now) {
        if (!amount.isPositive()) {
            throw new BusinessRuleViolationException(
                    GRANTED_AMOUNT_NOT_POSITIVE, "A granted amount is greater than zero");
        }
        if (amount.isGreaterThan(budget.remaining())) {
            throw new BusinessRuleViolationException(
                    GRANT_EXCEEDS_REMAINING_BUDGET,
                    "Grant of " + amount.amount() + " exceeds the remaining budget of "
                            + budget.remaining().amount() + " in call " + reference.value());
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

    public FundingCallReference reference() {
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
