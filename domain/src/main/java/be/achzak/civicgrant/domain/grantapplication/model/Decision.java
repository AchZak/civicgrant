package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public sealed interface Decision {

    UserId decidedBy();

    String motivation();

    Instant decidedAt();

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
