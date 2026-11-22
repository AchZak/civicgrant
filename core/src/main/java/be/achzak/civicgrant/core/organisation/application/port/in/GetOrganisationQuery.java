package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record GetOrganisationQuery(OrganisationId organisationId, Actor actor) implements Query {

    public GetOrganisationQuery {
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(actor, "actor");
    }
}
