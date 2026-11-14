package be.achzak.civicgrant.domain.grantapplication.model;

import java.time.Year;
import java.util.Objects;
import java.util.regex.Pattern;

public record ApplicationReference(String value) {

    private static final Pattern FORMAT = Pattern.compile("CG-\\d{4}-\\d{6}");

    public ApplicationReference {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("An application reference looks like CG-2026-000042: " + value);
        }
    }

    public static ApplicationReference of(Year year, long number) {
        Objects.requireNonNull(year, "year");
        return new ApplicationReference("CG-%d-%06d".formatted(year.getValue(), number));
    }
}
