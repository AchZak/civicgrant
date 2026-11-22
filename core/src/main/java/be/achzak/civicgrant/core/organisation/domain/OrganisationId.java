package be.achzak.civicgrant.core.organisation.domain;

import java.util.Objects;
import java.util.UUID;

public record OrganisationId(UUID value) {

    public OrganisationId {
        Objects.requireNonNull(value, "value");
    }

    public static OrganisationId generate() {
        return new OrganisationId(UUID.randomUUID());
    }
}
