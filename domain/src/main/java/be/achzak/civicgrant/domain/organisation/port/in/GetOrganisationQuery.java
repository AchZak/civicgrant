package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record GetOrganisationQuery(OrganisationId organisationId, Actor actor) {

    public GetOrganisationQuery {
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(actor, "actor");
    }
}
