package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import java.time.Instant;

public record ApplicationEdited(ApplicationId applicationId, Money requestedAmount, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {}
