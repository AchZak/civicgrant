package be.achzak.civicgrant.core.organisation.domain.event;

import be.achzak.civicgrant.core.common.domain.event.DomainEvent;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;

public sealed interface OrganisationEvent extends DomainEvent permits OrganisationRegistered {

    OrganisationId organisationId();
}
