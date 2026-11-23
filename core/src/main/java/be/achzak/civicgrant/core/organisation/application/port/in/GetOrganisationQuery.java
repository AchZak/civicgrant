package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record GetOrganisationQuery(OrganisationId organisationId, Actor actor) implements Query {}
