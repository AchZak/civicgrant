package be.achzak.civicgrant.domain.organisation.model;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EnterpriseNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("[01]\\d{9}");

    public EnterpriseNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleViolationException(
                    "ENTERPRISE_NUMBER_INVALID_FORMAT",
                    "Enterprise number must be 10 digits starting with 0 or 1: " + value);
        }
        long base = Long.parseLong(value.substring(0, 8));
        int checkDigits = Integer.parseInt(value.substring(8));
        if (checkDigits != 97 - base % 97) {
            throw new BusinessRuleViolationException(
                    "ENTERPRISE_NUMBER_INVALID_CHECKSUM", "Enterprise number fails the mod-97 check: " + value);
        }
    }

    public static EnterpriseNumber parse(String input) {
        Objects.requireNonNull(input, "input");
        String normalized = input.strip().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("BE")) {
            normalized = normalized.substring(2);
        }
        return new EnterpriseNumber(normalized.replaceAll("[\\s.]", ""));
    }
}
