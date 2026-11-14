package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.domain.shared.Money.euros;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationSnapshot;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.UserId;
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
