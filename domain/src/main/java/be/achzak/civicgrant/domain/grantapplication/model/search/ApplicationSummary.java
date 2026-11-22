package be.achzak.civicgrant.domain.grantapplication.model.search;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Money;
import java.time.Instant;
import java.util.Objects;

public record ApplicationSummary(
        ApplicationId id,
        ApplicationReference reference,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        ApplicationStatus status,
        String projectTitle,
        Money requestedAmount,
        Instant submittedAt,
        Instant updatedAt) {

    public ApplicationSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(projectTitle, "projectTitle");
        Objects.requireNonNull(requestedAmount, "requestedAmount");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
