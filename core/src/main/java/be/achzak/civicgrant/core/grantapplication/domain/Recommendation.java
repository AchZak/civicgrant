package be.achzak.civicgrant.core.grantapplication.domain;

import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.util.Objects;

public sealed interface Recommendation {

    record Approve(Money proposedAmount) implements Recommendation {

        public Approve {
            Objects.requireNonNull(proposedAmount, "proposedAmount");
        }
    }

    record Reject() implements Recommendation {}
}
