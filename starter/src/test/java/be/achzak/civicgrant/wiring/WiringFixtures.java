package be.achzak.civicgrant.wiring;

import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Language;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

class WiringFixtures {

    static final byte[] PDF = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    static final Actor DECISION_MAKER = new Actor(new UserId("decision-maker-1"), Set.of(Role.DECISION_MAKER));

    private final JdbcClient jdbc;
    private final RegisterOrganisationUseCase registerOrganisation;
    private final CreateApplicationUseCase createApplication;
    private final AttachDocumentUseCase attachDocument;
    private final SubmitApplicationUseCase submitApplication;
    private final RecordAssessmentUseCase recordAssessment;

    WiringFixtures(
            JdbcClient jdbc,
            RegisterOrganisationUseCase registerOrganisation,
            CreateApplicationUseCase createApplication,
            AttachDocumentUseCase attachDocument,
            SubmitApplicationUseCase submitApplication,
            RecordAssessmentUseCase recordAssessment) {
        this.jdbc = jdbc;
        this.registerOrganisation = registerOrganisation;
        this.createApplication = createApplication;
        this.attachDocument = attachDocument;
        this.submitApplication = submitApplication;
        this.recordAssessment = recordAssessment;
    }

    // Inserted with SQL around the database's now(), because the application's clock is the real one.
    FundingCallId anOpenFundingCall(String reference) {
        FundingCallId id = new FundingCallId(UUID.randomUUID());
        jdbc.sql("""
                        INSERT INTO civicgrant.funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                            total_budget, committed_amount, max_amount_per_application, version, created_at)
                        VALUES (:id, :reference, 'Buurtprojecten', 'Projets de quartier',
                            now() - interval '1 day', now() + interval '30 days', 50000.00, 0.00, 15000.00, 0, now())
                        """).param("id", id.value()).param("reference", reference).update();
        return id;
    }

    OrganisationId aRegisteredOrganisation(String enterpriseNumber, String name, Actor applicant) {
        return registerOrganisation.register(new RegisterOrganisationCommand(
                new EnterpriseNumber(enterpriseNumber), name, LegalForm.VZW_ASBL, applicant));
    }

    ApplicationId aSubmittedApplication(String callReference, String enterpriseNumber, Actor applicant) {
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall(callReference),
                aRegisteredOrganisation(enterpriseNumber, "Buurtwerk " + callReference, applicant),
                proposalTitled("Buurtproject " + callReference),
                applicant));
        attachDocument.attach(new AttachDocumentCommand(id, "projectplan.pdf", "application/pdf", PDF, applicant));
        submitApplication.submit(new SubmitApplicationCommand(id, applicant));
        return id;
    }

    ApplicationId anAssessedApplication(String callReference, String enterpriseNumber) {
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        Actor assessor = new Actor(new UserId("assessor-1"), Set.of(Role.ASSESSOR));
        ApplicationId id = aSubmittedApplication(callReference, enterpriseNumber, applicant);
        recordAssessment.record(new RecordAssessmentCommand(
                id, true, new Recommendation.Approve(euros("7500.00")), "Sterk buurtproject.", assessor));
        return id;
    }

    static ProjectProposal proposalTitled(String title) {
        return new ProjectProposal(
                title,
                "Vier weken activiteiten voor de buurt, met vrijwilligers uit de wijk.",
                euros("8000.00"),
                euros("12000.00"),
                Language.NL);
    }
}
