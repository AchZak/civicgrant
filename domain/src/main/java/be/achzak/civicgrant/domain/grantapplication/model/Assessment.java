package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record Assessment(
        UserId assessor, boolean eligible, Recommendation recommendation, String comment, Instant assessedAt) {

    public static final int MAX_COMMENT_LENGTH = 4_000;

    public Assessment {
        Objects.requireNonNull(assessor, "assessor");
        Objects.requireNonNull(recommendation, "recommendation");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(assessedAt, "assessedAt");
    }
}
