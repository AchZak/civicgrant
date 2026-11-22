package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_ALREADY_CLOSED;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_REFERENCE_INVALID;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_TITLE_TOO_LONG;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.GRANT_EXCEEDS_REMAINING_BUDGET;
import static be.achzak.civicgrant.domain.shared.SharedErrorCode.GRANTED_AMOUNT_NOT_POSITIVE;

import be.achzak.civicgrant.domain.fundingcall.model.event.FundingCallEvent;
import be.achzak.civicgrant.domain.fundingcall.model.event.FundingCallOpened;
import be.achzak.civicgrant.domain.fundingcall.model.event.GrantCommitted;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.error.BusinessRuleViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class FundingCall {

    private static final int MAX_REFERENCE_LENGTH = 30;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Pattern REFERENCE_CHARACTERS = Pattern.compile("[A-Z0-9-]{1," + MAX_REFERENCE_LENGTH + "}");

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

    public static FundingCall create(
            String reference,
            LocalizedText title,
            SubmissionPeriod submissionPeriod,
            Money totalBudget,
            Money capPerApplication,
            Actor openedBy,
            Instant now) {
        Objects.requireNonNull(submissionPeriod, "submissionPeriod");
        Objects.requireNonNull(openedBy, "openedBy");
        Objects.requireNonNull(now, "now");
        if (submissionPeriod.isClosedAt(now)) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_ALREADY_CLOSED, "A new funding call closes in the future");
        }
        BudgetEnvelope budget = BudgetEnvelope.unspent(totalBudget, capPerApplication);
        FundingCall fundingCall = new FundingCall(
                FundingCallId.generate(),
                validReference(reference),
                validTitle(title),
                submissionPeriod,
                budget,
                0,
                now);
        fundingCall.events.add(
                new FundingCallOpened(fundingCall.id, fundingCall.reference, budget.total(), openedBy.id(), now));
        return fundingCall;
    }

    private static String validReference(String reference) {
        String stripped = Objects.requireNonNull(reference, "reference").strip();
        if (!isWellFormedReference(stripped)) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_REFERENCE_INVALID,
                    "A funding call reference is up to " + MAX_REFERENCE_LENGTH
                            + " capitals and digits, in groups joined by hyphens: " + stripped);
        }
        return stripped;
    }

    private static boolean isWellFormedReference(String reference) {
        return REFERENCE_CHARACTERS.matcher(reference).matches()
                && !reference.startsWith("-")
                && !reference.endsWith("-")
                && !reference.contains("--");
    }

    private static LocalizedText validTitle(LocalizedText title) {
        Objects.requireNonNull(title, "title");
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
                    GRANTED_AMOUNT_NOT_POSITIVE, "A granted amount is greater than zero");
        }
        if (amount.isGreaterThan(budget.remaining())) {
            throw new BusinessRuleViolationException(
                    GRANT_EXCEEDS_REMAINING_BUDGET,
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
