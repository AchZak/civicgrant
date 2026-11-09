package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GrantApplicationSnapshot(
        ApplicationId id,
        ApplicationReference reference,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        UserId applicant,
        ProjectProposal proposal,
        List<SupportingDocument> documents,
        ApplicationStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public GrantApplicationSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(applicant, "applicant");
        Objects.requireNonNull(proposal, "proposal");
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if ((status == ApplicationStatus.DRAFT) != (submittedAt == null)) {
            throw new IllegalArgumentException("Application " + reference.value() + " is " + status
                    + (submittedAt == null ? " without" : " with") + " a submission time");
        }
    }
}
