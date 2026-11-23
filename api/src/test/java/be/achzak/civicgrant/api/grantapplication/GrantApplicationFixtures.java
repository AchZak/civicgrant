package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.core.common.domain.Money.euros;

import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationReference;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.grantapplication.domain.Decision;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationSnapshot;
import be.achzak.civicgrant.core.grantapplication.domain.Language;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class GrantApplicationFixtures {

    static final ApplicationId APPLICATION_ID =
            new ApplicationId(UUID.fromString("a9917c01-0001-4000-8000-000000000001"));
    static final FundingCallId FUNDING_CALL_ID =
            new FundingCallId(UUID.fromString("a9917c01-0002-4000-8000-000000000002"));
    static final OrganisationId ORGANISATION_ID =
            new OrganisationId(UUID.fromString("a9917c01-0003-4000-8000-000000000003"));
    static final Instant CREATED_AT = Instant.parse("2026-04-13T09:00:00Z");
    static final Instant SUBMITTED_AT = Instant.parse("2026-04-20T15:30:00Z");
    static final Instant ASSESSED_AT = Instant.parse("2026-05-04T14:00:00Z");
    static final Instant DECIDED_AT = Instant.parse("2026-05-18T09:30:00Z");
    private static final UserId DECISION_MAKER = new UserId("decision-maker-1");

    private GrantApplicationFixtures() {}

    static ProjectProposal aProposal(Language language) {
        return new ProjectProposal(
                "Samen tuinieren in de wijk",
                "Een gedeelde moestuin voor de buurtbewoners.",
                euros("8000.00"),
                euros("12000.00"),
                language);
    }

    static GrantApplication aDraft(ProjectProposal proposal) {
        return application(proposal, ApplicationStatus.DRAFT, null, CREATED_AT);
    }

    static GrantApplication aSubmittedApplication() {
        return application(aProposal(Language.NL), ApplicationStatus.SUBMITTED, SUBMITTED_AT, SUBMITTED_AT);
    }

    static GrantApplication anAssessedApplication() {
        return application(ApplicationStatus.ASSESSED, null, ASSESSED_AT);
    }

    static GrantApplication anApprovedApplication() {
        return application(
                ApplicationStatus.APPROVED,
                new Decision.Approval(euros("7000.00"), DECISION_MAKER, "Het project versterkt de buurt.", DECIDED_AT),
                DECIDED_AT);
    }

    static GrantApplication aRejectedApplication() {
        return application(
                ApplicationStatus.REJECTED,
                new Decision.Rejection(
                        RejectionGround.INSUFFICIENT_QUALITY,
                        DECISION_MAKER,
                        "Het projectplan is onvoldoende uitgewerkt.",
                        DECIDED_AT),
                DECIDED_AT);
    }

    static ApplicationSummary aSummary() {
        return new ApplicationSummary(
                APPLICATION_ID,
                new ApplicationReference("CG-2026-000042"),
                FUNDING_CALL_ID,
                ORGANISATION_ID,
                ApplicationStatus.SUBMITTED,
                "Samen tuinieren in de wijk",
                euros("8000.00"),
                SUBMITTED_AT,
                SUBMITTED_AT);
    }

    private static GrantApplication application(ApplicationStatus status, Decision decision, Instant updatedAt) {
        return GrantApplication.restore(new GrantApplicationSnapshot(
                APPLICATION_ID,
                new ApplicationReference("CG-2026-000042"),
                FUNDING_CALL_ID,
                ORGANISATION_ID,
                new UserId("3f1c2b7e-5a4d-4c3b-9e2f-1a0b9c8d7e6f"),
                aProposal(Language.NL),
                List.of(),
                status,
                SUBMITTED_AT,
                new Assessment(
                        new UserId("assessor-1"),
                        true,
                        new Recommendation.Approve(euros("7500.00")),
                        "Sterk buurtproject.",
                        ASSESSED_AT),
                decision,
                CREATED_AT,
                updatedAt,
                0));
    }

    private static GrantApplication application(
            ProjectProposal proposal, ApplicationStatus status, Instant submittedAt, Instant updatedAt) {
        return GrantApplication.restore(new GrantApplicationSnapshot(
                APPLICATION_ID,
                new ApplicationReference("CG-2026-000042"),
                FUNDING_CALL_ID,
                ORGANISATION_ID,
                new UserId("3f1c2b7e-5a4d-4c3b-9e2f-1a0b9c8d7e6f"),
                proposal,
                List.of(),
                status,
                submittedAt,
                null,
                null,
                CREATED_AT,
                updatedAt,
                0));
    }
}
