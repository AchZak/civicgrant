package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.CAP_PER_APPLICATION_EXCEEDS_BUDGET;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.CAP_PER_APPLICATION_NOT_POSITIVE;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.TOTAL_BUDGET_NOT_POSITIVE;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.error.BusinessRuleViolationException;
import java.util.Objects;

public record BudgetEnvelope(Money total, Money committed, Money capPerApplication) {

    public BudgetEnvelope {
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(committed, "committed");
        Objects.requireNonNull(capPerApplication, "capPerApplication");
        if (!total.isPositive()) {
            throw new BusinessRuleViolationException(
                    TOTAL_BUDGET_NOT_POSITIVE, "The total budget is greater than zero");
        }
        if (!capPerApplication.isPositive()) {
            throw new BusinessRuleViolationException(
                    CAP_PER_APPLICATION_NOT_POSITIVE, "The cap per application is greater than zero");
        }
        if (capPerApplication.isGreaterThan(total)) {
            throw new BusinessRuleViolationException(
                    CAP_PER_APPLICATION_EXCEEDS_BUDGET, "The cap per application can't exceed the total budget");
        }
        // Only commitGrant changes the committed amount, and it checks first: exceeding the total is a bug.
        if (committed.isGreaterThan(total)) {
            throw new IllegalArgumentException("The committed amount can't exceed the total budget");
        }
    }

    public static BudgetEnvelope unspent(Money total, Money capPerApplication) {
        return new BudgetEnvelope(total, Money.ZERO, capPerApplication);
    }

    public Money remaining() {
        return total.minus(committed);
    }

    BudgetEnvelope commit(Money amount) {
        return new BudgetEnvelope(total, committed.plus(amount), capPerApplication);
    }
}
