package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        Assessment assessment,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    private static final Set<ApplicationStatus> ASSESSED_OR_LATER =
            EnumSet.of(ApplicationStatus.ASSESSED, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED);

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
        if (ASSESSED_OR_LATER.contains(status) != (assessment != null)) {
            throw new IllegalArgumentException("Application " + reference.value() + " is " + status
                    + (assessment == null ? " without" : " with") + " an assessment");
        }
    }
}
