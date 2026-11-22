package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.domain.event.DomainEvent;

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
