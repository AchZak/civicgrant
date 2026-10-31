package be.achzak.civicgrant.domain.shared;

import java.util.Objects;

public record UserId(String value) {

    private static final int MAX_LENGTH = 64;

    public UserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("A user id has 1 to " + MAX_LENGTH + " characters");
        }
    }
}
