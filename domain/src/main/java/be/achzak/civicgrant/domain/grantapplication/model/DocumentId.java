package be.achzak.civicgrant.domain.grantapplication.model;

import java.util.Objects;
import java.util.UUID;

public record DocumentId(UUID value) {

    public DocumentId {
        Objects.requireNonNull(value, "value");
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }
}
