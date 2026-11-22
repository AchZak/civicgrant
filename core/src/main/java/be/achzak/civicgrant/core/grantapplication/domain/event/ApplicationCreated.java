package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;
import java.util.Objects;

public record ApplicationCreated(
        ApplicationId applicationId,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        UserId actorId,
        Instant occurredAt)
        implements GrantApplicationEvent {

    public ApplicationCreated {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
