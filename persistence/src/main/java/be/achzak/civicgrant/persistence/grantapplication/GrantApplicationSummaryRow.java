package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record GrantApplicationSummaryRow(
        UUID id,
        String reference,
        UUID fundingCallId,
        UUID organisationId,
        ApplicationStatus status,
        String projectTitle,
        BigDecimal requestedAmount,
        Instant submittedAt,
        Instant updatedAt) {}
