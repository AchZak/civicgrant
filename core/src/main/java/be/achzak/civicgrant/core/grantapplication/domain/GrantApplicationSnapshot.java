package be.achzak.civicgrant.core.grantapplication.domain;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record GrantApplicationSnapshot(
        ApplicationId id,
        ApplicationReference reference,
        FundingCallId fundingCallId,
        OrganisationId organisationId,
        UserId applicant,
        ProjectProposal proposal,
        List<SupportingDocument> documents,
        ApplicationStatus status,
        @Nullable Instant submittedAt,
        @Nullable Assessment assessment,
        @Nullable Decision decision,
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
        boolean decisionMatchesStatus = switch (status) {
            case DRAFT, SUBMITTED, ASSESSED -> decision == null;
            case APPROVED -> decision instanceof Decision.Approval;
            case REJECTED -> decision instanceof Decision.Rejection;
        };
        if (!decisionMatchesStatus) {
            throw new IllegalArgumentException("Application " + reference.value() + " is " + status + " with decision "
                    + (decision == null ? "none" : decision.getClass().getSimpleName()));
        }
    }
}
