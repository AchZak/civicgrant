package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
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
