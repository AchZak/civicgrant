package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.Money;
import java.util.Objects;

public record ProjectProposal(
        String title, String description, Money requestedAmount, Money totalProjectBudget, Language language) {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 10_000;

    public ProjectProposal {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(requestedAmount, "requestedAmount");
        Objects.requireNonNull(totalProjectBudget, "totalProjectBudget");
        Objects.requireNonNull(language, "language");
        title = RequiredText.strip(title, MAX_TITLE_LENGTH, "PROJECT_TITLE_REQUIRED", "PROJECT_TITLE_TOO_LONG");
        description = RequiredText.strip(
                description, MAX_DESCRIPTION_LENGTH, "PROJECT_DESCRIPTION_REQUIRED", "PROJECT_DESCRIPTION_TOO_LONG");
        if (!requestedAmount.isPositive()) {
            throw new BusinessRuleViolationException(
                    "REQUESTED_AMOUNT_NOT_POSITIVE", "A requested amount is greater than zero");
        }
        if (requestedAmount.isGreaterThan(totalProjectBudget)) {
            throw new BusinessRuleViolationException(
                    "REQUESTED_AMOUNT_EXCEEDS_PROJECT_BUDGET",
                    "Requested amount " + requestedAmount.amount() + " exceeds the project budget of "
                            + totalProjectBudget.amount());
        }
    }
}
