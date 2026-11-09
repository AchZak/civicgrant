package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.DomainEvent;

public sealed interface GrantApplicationEvent extends DomainEvent
        permits ApplicationCreated, ApplicationEdited, ApplicationSubmitted {

    ApplicationId applicationId();
}
