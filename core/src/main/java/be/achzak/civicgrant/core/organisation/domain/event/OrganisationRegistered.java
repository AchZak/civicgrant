package be.achzak.civicgrant.core.organisation.domain.event;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.time.Instant;

public record OrganisationRegistered(
        OrganisationId organisationId, EnterpriseNumber enterpriseNumber, UserId actorId, Instant occurredAt)
        implements OrganisationEvent {}
