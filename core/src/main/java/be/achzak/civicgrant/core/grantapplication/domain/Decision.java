package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.GRANTED_AMOUNT_NOT_POSITIVE;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.GRANTED_AMOUNT_EXCEEDS_REQUESTED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.MOTIVATION_REQUIRED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.MOTIVATION_TOO_LONG;

import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public sealed interface Decision {

    int MAX_MOTIVATION_LENGTH = 4_000;

    UserId decidedBy();

    String motivation();

    Instant decidedAt();

    static Approval approval(
            Money grantedAmount, Money requestedAmount, UserId decidedBy, String motivation, Instant decidedAt) {
        String validMotivation = validMotivation(motivation);
        if (!grantedAmount.isPositive()) {
            throw new BusinessRuleViolationException(
                    GRANTED_AMOUNT_NOT_POSITIVE, "A granted amount is greater than zero");
        }
        if (grantedAmount.isGreaterThan(requestedAmount)) {
            throw new BusinessRuleViolationException(
                    GRANTED_AMOUNT_EXCEEDS_REQUESTED,
                    "Granted amount " + grantedAmount.amount() + " exceeds the requested amount of "
                            + requestedAmount.amount());
        }
        return new Approval(grantedAmount, decidedBy, validMotivation, decidedAt);
    }

    static Rejection rejection(RejectionGround ground, UserId decidedBy, String motivation, Instant decidedAt) {
        return new Rejection(ground, decidedBy, validMotivation(motivation), decidedAt);
    }

    private static String validMotivation(String motivation) {
        return RequiredText.strip(motivation, MAX_MOTIVATION_LENGTH, MOTIVATION_REQUIRED, MOTIVATION_TOO_LONG);
    }

    record Approval(Money grantedAmount, UserId decidedBy, String motivation, Instant decidedAt) implements Decision {

        public Approval {
            Objects.requireNonNull(grantedAmount, "grantedAmount");
            Objects.requireNonNull(decidedBy, "decidedBy");
            Objects.requireNonNull(motivation, "motivation");
            Objects.requireNonNull(decidedAt, "decidedAt");
        }
    }

    record Rejection(RejectionGround ground, UserId decidedBy, String motivation, Instant decidedAt)
            implements Decision {

        public Rejection {
            Objects.requireNonNull(ground, "ground");
            Objects.requireNonNull(decidedBy, "decidedBy");
            Objects.requireNonNull(motivation, "motivation");
            Objects.requireNonNull(decidedAt, "decidedAt");
        }
    }
}
