package be.achzak.civicgrant.domain.organisation.model;

import be.achzak.civicgrant.domain.shared.DomainEvent;

public sealed interface OrganisationEvent extends DomainEvent permits OrganisationRegistered {

    OrganisationId organisationId();
}
