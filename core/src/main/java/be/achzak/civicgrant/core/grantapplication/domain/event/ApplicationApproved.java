package be.achzak.civicgrant.core.grantapplication.domain.event;

import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import java.time.Instant;

public record ApplicationApproved(ApplicationId applicationId, Money grantedAmount, UserId actorId, Instant occurredAt)
        implements GrantApplicationEvent {}
