package be.achzak.civicgrant.domain.organisation.model.event;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Objects;

public record OrganisationRegistered(
        OrganisationId organisationId, EnterpriseNumber enterpriseNumber, UserId actorId, Instant occurredAt)
        implements OrganisationEvent {

    public OrganisationRegistered {
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(enterpriseNumber, "enterpriseNumber");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
