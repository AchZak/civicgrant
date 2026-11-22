package be.achzak.civicgrant.domain.grantapplication.model.event;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.shared.event.DomainEvent;

public sealed interface GrantApplicationEvent extends DomainEvent
        permits ApplicationCreated,
                ApplicationEdited,
                DocumentAttached,
                DocumentRemoved,
                ApplicationSubmitted,
                AssessmentRecorded,
                ApplicationApproved,
                ApplicationRejected {

    ApplicationId applicationId();
}
