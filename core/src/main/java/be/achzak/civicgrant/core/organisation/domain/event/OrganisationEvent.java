package be.achzak.civicgrant.core.organisation.domain.event;

import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.event.DomainEvent;

public sealed interface OrganisationEvent extends DomainEvent permits OrganisationRegistered {

    OrganisationId organisationId();
}
