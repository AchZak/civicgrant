package be.achzak.civicgrant.core.fundingcall.domain;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode.FUNDING_CALL_REFERENCE_INVALID;

import be.achzak.civicgrant.core.sharedkernel.domain.error.BusinessRuleViolationException;
import java.util.Objects;
import java.util.regex.Pattern;

public record FundingCallReference(String value) {

    private static final int MAX_LENGTH = 30;
    private static final Pattern CHARACTERS = Pattern.compile("[A-Z0-9-]{1," + MAX_LENGTH + "}");

    public FundingCallReference {
        Objects.requireNonNull(value, "value");
        if (!isWellFormed(value)) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_REFERENCE_INVALID,
                    "A funding call reference is up to " + MAX_LENGTH
                            + " capitals and digits, in groups joined by hyphens: " + value);
        }
    }

    public static FundingCallReference parse(String input) {
        return new FundingCallReference(input.strip());
    }

    // Checked in steps rather than as one repeated group, which Java's regex engine would recurse on.
    private static boolean isWellFormed(String value) {
        return CHARACTERS.matcher(value).matches()
                && !value.startsWith("-")
                && !value.endsWith("-")
                && !value.contains("--");
    }
}
