package be.achzak.civicgrant.core.common.domain;

import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.AMOUNT_NEGATIVE;
import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.AMOUNT_TOO_PRECISE;

import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new BusinessRuleViolationException(
                    AMOUNT_NEGATIVE, "Amount can't be negative: " + amount.toPlainString());
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BusinessRuleViolationException(
                    AMOUNT_TOO_PRECISE, "Amount has more than two decimals: " + amount.toPlainString());
        }
    }

    public static Money euros(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
