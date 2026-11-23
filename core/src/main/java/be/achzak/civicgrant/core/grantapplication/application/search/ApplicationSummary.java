package be.achzak.civicgrant.core.grantapplication.application.search;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationReference;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record ApplicationSummary(
        ApplicationId id,
        ApplicationReference reference,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        ApplicationStatus status,
        String projectTitle,
        Money requestedAmount,
        @Nullable Instant submittedAt,
        Instant updatedAt) {}
