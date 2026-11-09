package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;

public final class GrantApplicationBuilder {

    public static final Instant CREATED_AT = Instant.parse("2026-03-10T09:00:00Z");
    public static final Instant SUBMITTED_AT = Instant.parse("2026-04-01T10:00:00Z");

    private final ApplicationStatus status;
    private ApplicationId id = ApplicationId.generate();
    private FundingCallId fundingCallId = FundingCallId.generate();
    private UserId applicant = anApplicant().id();
    private ProjectProposal proposal = aProjectProposal().build();

    private GrantApplicationBuilder(ApplicationStatus status) {
        this.status = status;
    }

    public static GrantApplicationBuilder aDraftApplication() {
        return new GrantApplicationBuilder(ApplicationStatus.DRAFT);
    }

    public static GrantApplicationBuilder aSubmittedApplication() {
        return new GrantApplicationBuilder(ApplicationStatus.SUBMITTED);
    }

    public GrantApplicationBuilder withId(ApplicationId id) {
        this.id = id;
        return this;
    }

    public GrantApplicationBuilder forCall(FundingCall fundingCall) {
        this.fundingCallId = fundingCall.id();
        return this;
    }

    public GrantApplicationBuilder withApplicant(UserId applicant) {
        this.applicant = applicant;
        return this;
    }

    public GrantApplicationBuilder withProposal(ProjectProposal proposal) {
        this.proposal = proposal;
        return this;
    }

    public GrantApplication build() {
        boolean draft = status == ApplicationStatus.DRAFT;
        return GrantApplication.restore(new GrantApplicationSnapshot(
                id,
                new ApplicationReference("CG-2026-000042"),
                fundingCallId,
                OrganisationId.generate(),
                applicant,
                proposal,
                status,
                draft ? null : SUBMITTED_AT,
                CREATED_AT,
                draft ? CREATED_AT : SUBMITTED_AT,
                0));
    }
}
