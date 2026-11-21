package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Query;
import java.util.Objects;

public record GetOrganisationQuery(OrganisationId organisationId, Actor actor) implements Query {

    public GetOrganisationQuery {
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(actor, "actor");
    }
}
