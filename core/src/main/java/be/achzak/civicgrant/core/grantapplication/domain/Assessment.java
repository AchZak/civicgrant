package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPROVAL_RECOMMENDATION_REQUIRES_ELIGIBILITY;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.ASSESSMENT_COMMENT_REQUIRED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.ASSESSMENT_COMMENT_TOO_LONG;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.PROPOSED_AMOUNT_EXCEEDS_REQUESTED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.PROPOSED_AMOUNT_NOT_POSITIVE;

import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
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

    public static Assessment record(
            UserId assessor,
            boolean eligible,
            Recommendation recommendation,
            String comment,
            Money requestedAmount,
            Instant assessedAt) {
        String validComment = RequiredText.strip(
                comment, MAX_COMMENT_LENGTH, ASSESSMENT_COMMENT_REQUIRED, ASSESSMENT_COMMENT_TOO_LONG);
        if (recommendation instanceof Recommendation.Approve(Money proposedAmount)) {
            ensureApprovalCanBeRecommended(eligible, proposedAmount, requestedAmount);
        }
        return new Assessment(assessor, eligible, recommendation, validComment, assessedAt);
    }

    private static void ensureApprovalCanBeRecommended(boolean eligible, Money proposedAmount, Money requestedAmount) {
        if (!eligible) {
            throw new BusinessRuleViolationException(
                    APPROVAL_RECOMMENDATION_REQUIRES_ELIGIBILITY,
                    "An ineligible application can't be recommended for approval");
        }
        if (!proposedAmount.isPositive()) {
            throw new BusinessRuleViolationException(
                    PROPOSED_AMOUNT_NOT_POSITIVE, "A proposed amount is greater than zero");
        }
        if (proposedAmount.isGreaterThan(requestedAmount)) {
            throw new BusinessRuleViolationException(
                    PROPOSED_AMOUNT_EXCEEDS_REQUESTED,
                    "Proposed amount " + proposedAmount.amount() + " exceeds the requested amount of "
                            + requestedAmount.amount());
        }
    }
}
