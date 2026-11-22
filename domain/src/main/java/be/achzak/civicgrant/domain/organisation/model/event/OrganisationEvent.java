package be.achzak.civicgrant.domain.organisation.model.event;

import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.event.DomainEvent;

public sealed interface OrganisationEvent extends DomainEvent permits OrganisationRegistered {

    OrganisationId organisationId();
}
