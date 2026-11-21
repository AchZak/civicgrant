package be.achzak.civicgrant.domain.fundingcall.model;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import java.time.Instant;
import java.util.Objects;

public record SubmissionPeriod(Instant opensAt, Instant closesAt) {

    public SubmissionPeriod {
        Objects.requireNonNull(opensAt, "opensAt");
        Objects.requireNonNull(closesAt, "closesAt");
        if (!closesAt.isAfter(opensAt)) {
            throw new BusinessRuleViolationException(
                    "SUBMISSION_PERIOD_CLOSES_BEFORE_OPENING", "A submission period closes after it opens");
        }
    }

    public boolean isUpcomingAt(Instant moment) {
        return moment.isBefore(opensAt);
    }

    public boolean isClosedAt(Instant moment) {
        return !moment.isBefore(closesAt);
    }

    public boolean isOpenAt(Instant moment) {
        return !isUpcomingAt(moment) && !isClosedAt(moment);
    }
}
