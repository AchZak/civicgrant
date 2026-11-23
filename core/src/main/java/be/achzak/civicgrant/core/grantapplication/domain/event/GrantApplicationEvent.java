package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.event.DomainEvent;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;

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
