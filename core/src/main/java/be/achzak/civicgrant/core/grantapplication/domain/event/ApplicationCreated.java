package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.time.Instant;

public record ApplicationCreated(
        ApplicationId applicationId,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        UserId actorId,
        Instant occurredAt)
        implements GrantApplicationEvent {}
