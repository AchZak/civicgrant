package be.achzak.civicgrant.domain.grantapplication.model;

import java.util.Objects;
import java.util.UUID;

public record ApplicationId(UUID value) {

    public ApplicationId {
        Objects.requireNonNull(value, "value");
    }

    public static ApplicationId generate() {
        return new ApplicationId(UUID.randomUUID());
    }
}
