package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.Money;
import java.util.Objects;

public record BudgetEnvelope(Money total, Money committed, Money capPerApplication) {

    public BudgetEnvelope {
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(committed, "committed");
        Objects.requireNonNull(capPerApplication, "capPerApplication");
        if (!total.isPositive()) {
            throw new IllegalArgumentException("The total budget is greater than zero");
        }
        if (committed.isGreaterThan(total)) {
            throw new IllegalArgumentException("The committed amount can't exceed the total budget");
        }
        if (!capPerApplication.isPositive() || capPerApplication.isGreaterThan(total)) {
            throw new IllegalArgumentException("The cap per application is between zero and the total budget");
        }
    }

    public Money remaining() {
        return total.minus(committed);
    }

    BudgetEnvelope commit(Money amount) {
        return new BudgetEnvelope(total, committed.plus(amount), capPerApplication);
    }
}
