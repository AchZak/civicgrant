package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Money;
import java.util.Objects;

public sealed interface Recommendation {

    record Approve(Money proposedAmount) implements Recommendation {

        public Approve {
            Objects.requireNonNull(proposedAmount, "proposedAmount");
        }
    }

    record Reject() implements Recommendation {}
}
