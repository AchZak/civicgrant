package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.List;

public final class GrantApplicationBuilder {

    public static final Instant CREATED_AT = Instant.parse("2026-03-10T09:00:00Z");
    public static final Instant SUBMITTED_AT = Instant.parse("2026-04-01T10:00:00Z");
    public static final Instant ASSESSED_AT = Instant.parse("2026-05-04T14:00:00Z");

    private final ApplicationStatus status;
    private ApplicationId id = ApplicationId.generate();
    private FundingCallId fundingCallId = FundingCallId.generate();
    private UserId applicant = anApplicant().id();
    private ProjectProposal proposal = aProjectProposal().build();
    private List<SupportingDocument> documents = List.of(aSupportingDocument());
    private Assessment assessment;

    private GrantApplicationBuilder(ApplicationStatus status) {
        this.status = status;
    }

    public static GrantApplicationBuilder aDraftApplication() {
        return new GrantApplicationBuilder(ApplicationStatus.DRAFT);
    }

    public static GrantApplicationBuilder aSubmittedApplication() {
        return new GrantApplicationBuilder(ApplicationStatus.SUBMITTED);
    }

    public static GrantApplicationBuilder anAssessedApplication() {
        GrantApplicationBuilder builder = new GrantApplicationBuilder(ApplicationStatus.ASSESSED);
        builder.assessment = new Assessment(
                anAssessor().id(),
                true,
                new Recommendation.Approve(builder.proposal.requestedAmount()),
                "Sterk buurtproject met een realistische begroting.",
                ASSESSED_AT);
        return builder;
    }

    public static SupportingDocument aSupportingDocument() {
        return new SupportingDocument(
                DocumentId.generate(), "begroting.pdf", 245_760, anApplicant().id(), CREATED_AT);
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

    public GrantApplicationBuilder withDocuments(SupportingDocument... documents) {
        this.documents = List.of(documents);
        return this;
    }

    public GrantApplicationBuilder withoutDocuments() {
        this.documents = List.of();
        return this;
    }

    public GrantApplicationBuilder withAssessment(Assessment assessment) {
        this.assessment = assessment;
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
                documents,
                status,
                draft ? null : SUBMITTED_AT,
                assessment,
                CREATED_AT,
                lastChange(),
                0));
    }

    private Instant lastChange() {
        return switch (status) {
            case DRAFT -> CREATED_AT;
            case SUBMITTED -> SUBMITTED_AT;
            case ASSESSED, APPROVED, REJECTED -> ASSESSED_AT;
        };
    }
}
