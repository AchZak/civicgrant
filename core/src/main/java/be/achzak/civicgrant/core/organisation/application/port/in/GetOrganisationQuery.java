package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Query;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;

public record GetOrganisationQuery(OrganisationId organisationId, Actor actor) implements Query {}
