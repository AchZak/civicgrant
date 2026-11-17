package be.achzak.civicgrant;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.DocumentStorage;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import jakarta.servlet.MultipartConfigElement;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test/v2.0",
            "spring.security.oauth2.resourceserver.jwt.audiences=civicgrant-api-test"
        })
@DisplayName("CivicGrant application")
class CivicgrantApplicationTests {

    private static final byte[] PDF = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private RegisterOrganisationUseCase registerOrganisation;

    @Autowired
    private SearchOrganisationsUseCase searchOrganisations;

    @Autowired
    private CreateApplicationUseCase createApplication;

    @Autowired
    private EditApplicationUseCase editApplication;

    @Autowired
    private AttachDocumentUseCase attachDocument;

    @Autowired
    private RemoveDocumentUseCase removeDocument;

    @Autowired
    private SubmitApplicationUseCase submitApplication;

    @Autowired
    private RecordAssessmentUseCase recordAssessment;

    @Autowired
    private DocumentStorage documents;

    @Autowired
    private MultipartConfigElement multipartConfig;

    @Autowired
    private JdbcClient jdbc;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("starts the full context against PostgreSQL")
    void contextLoads() {}

    @Test
    @DisplayName("registers an organisation through the wired use cases and finds it again")
    void registersAnOrganisationThroughTheWiredUseCasesAndFindsItAgain() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        EnterpriseNumber enterpriseNumber = new EnterpriseNumber("0202239951");
        registerOrganisation.register(
                new RegisterOrganisationCommand(enterpriseNumber, "Buurtwerk vzw", LegalForm.VZW_ASBL, applicant));

        // when
        List<Organisation> found =
                searchOrganisations.search(new SearchOrganisationsQuery(enterpriseNumber, applicant));

        // then
        assertThat(found).extracting(Organisation::name).containsExactly("Buurtwerk vzw");
    }

    @Test
    @DisplayName("writes an audit entry when an organisation is registered")
    void writesAnAuditEntryWhenAnOrganisationIsRegistered() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        RegisterOrganisationCommand registration = new RegisterOrganisationCommand(
                new EnterpriseNumber("0412345614"), "Jeugdhuis De Kroon", LegalForm.VZW_ASBL, applicant);

        // when
        OrganisationId id = registerOrganisation.register(registration);

        // then
        assertThat(jdbc.sql("SELECT event_type FROM civicgrant.audit_event WHERE organisation_id = :id")
                        .param("id", id.value())
                        .query(String.class)
                        .list())
                .containsExactly("ORGANISATION_REGISTERED");
    }

    @Test
    @DisplayName("creates a numbered application and writes its audit entry")
    void createsANumberedApplicationAndWritesItsAuditEntry() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        FundingCallId fundingCall = anOpenFundingCall("NBH-CREATE");
        OrganisationId organisation = aRegisteredOrganisation("0476543281", "Speelpleinwerking Zonnebloem", applicant);

        // when
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                fundingCall, organisation, proposalTitled("Speelplein in de zomer"), applicant));

        // then
        assertThat(auditedEventTypesOf(id)).containsExactly("APPLICATION_CREATED");
    }

    @Test
    @DisplayName("stores an edited draft and writes its audit entry")
    void storesAnEditedDraftAndWritesItsAuditEntry() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall("NBH-EDIT"),
                aRegisteredOrganisation("0428123455", "Repaircafe De Schroef", applicant),
                proposalTitled("Repaircafe in het buurthuis"),
                applicant));

        // when
        editApplication.edit(new EditApplicationCommand(id, proposalTitled("Repaircafe met fietsherstel"), applicant));

        // then
        assertThat(jdbc.sql("SELECT project_title FROM civicgrant.grant_application WHERE id = :id")
                        .param("id", id.value())
                        .query(String.class)
                        .single())
                .isEqualTo("Repaircafe met fietsherstel");
        assertThat(auditedEventTypesOf(id)).containsExactlyInAnyOrder("APPLICATION_CREATED", "APPLICATION_EDITED");
    }

    @Test
    @DisplayName("stores an attached document with its file and writes its audit entry")
    void storesAnAttachedDocumentWithItsFileAndWritesItsAuditEntry() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall("NBH-ATTACH"),
                aRegisteredOrganisation("0431000001", "Buurtmoestuin De Kiem", applicant),
                proposalTitled("Moestuin in de wijk"),
                applicant));

        // when
        DocumentId document = attachDocument.attach(
                new AttachDocumentCommand(id, "begroting.pdf", "application/pdf", PDF, applicant));

        // then
        assertThat(jdbc.sql("SELECT original_filename FROM civicgrant.supporting_document WHERE id = :id")
                        .param("id", document.value())
                        .query(String.class)
                        .single())
                .isEqualTo("begroting.pdf");
        assertThat(documents.open(id, document)).hasBinaryContent(PDF);
        assertThat(auditedEventTypesOf(id)).containsExactlyInAnyOrder("APPLICATION_CREATED", "DOCUMENT_ATTACHED");
    }

    @Test
    @DisplayName("deletes a removed document's file once the removal is committed")
    void deletesARemovedDocumentsFileOnceTheRemovalIsCommitted() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall("NBH-REMOVE"),
                aRegisteredOrganisation("0432000089", "Fietsatelier De Spaak", applicant),
                proposalTitled("Fietsatelier voor de buurt"),
                applicant));
        DocumentId document =
                attachDocument.attach(new AttachDocumentCommand(id, "offerte.pdf", "application/pdf", PDF, applicant));

        // when
        removeDocument.remove(new RemoveDocumentCommand(id, document, applicant));

        // then
        assertThat(jdbc.sql("SELECT count(*) FROM civicgrant.supporting_document WHERE id = :id")
                        .param("id", document.value())
                        .query(Long.class)
                        .single())
                .isZero();
        assertThat(catchThrowable(() -> documents.open(id, document).readAllBytes()))
                .isInstanceOfSatisfying(
                        BlobStorageException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(BlobErrorCode.BLOB_NOT_FOUND));
        assertThat(auditedEventTypesOf(id))
                .containsExactlyInAnyOrder("APPLICATION_CREATED", "DOCUMENT_ATTACHED", "DOCUMENT_REMOVED");
    }

    @Test
    @DisplayName("stores a submitted application and writes its audit entry")
    void storesASubmittedApplicationAndWritesItsAuditEntry() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall("NBH-SUBMIT"),
                aRegisteredOrganisation("0433000080", "Buurtkeuken De Pollepel", applicant),
                proposalTitled("Buurtkeuken voor iedereen"),
                applicant));
        attachDocument.attach(new AttachDocumentCommand(id, "projectplan.pdf", "application/pdf", PDF, applicant));

        // when
        submitApplication.submit(new SubmitApplicationCommand(id, applicant));

        // then
        assertThat(jdbc.sql("SELECT status FROM civicgrant.grant_application WHERE id = :id")
                        .param("id", id.value())
                        .query(String.class)
                        .single())
                .isEqualTo("SUBMITTED");
        assertThat(auditedEventTypesOf(id))
                .containsExactlyInAnyOrder("APPLICATION_CREATED", "DOCUMENT_ATTACHED", "APPLICATION_SUBMITTED");
    }

    @Test
    @DisplayName("stores an assessment and writes its audit entry")
    void storesAnAssessmentAndWritesItsAuditEntry() {
        // given
        Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
        Actor assessor = new Actor(new UserId("assessor-1"), Set.of(Role.ASSESSOR));
        ApplicationId id = aSubmittedApplication("NBH-ASSESS", "0433000179", applicant);

        // when
        recordAssessment.record(new RecordAssessmentCommand(
                id, true, new Recommendation.Approve(euros("7500.00")), "Sterk buurtproject.", assessor));

        // then
        assertThat(jdbc.sql("""
                        SELECT a.status || ' ' || s.assessor_oid || ' ' || s.recommendation || ' ' || s.proposed_amount
                        FROM civicgrant.grant_application a JOIN civicgrant.assessment s ON s.grant_application_id = a.id
                        WHERE a.id = :id
                        """).param("id", id.value()).query(String.class).single())
                .isEqualTo("ASSESSED assessor-1 APPROVE 7500.00");
        assertThat(auditedEventTypesOf(id))
                .containsExactlyInAnyOrder(
                        "APPLICATION_CREATED", "DOCUMENT_ATTACHED", "APPLICATION_SUBMITTED", "ASSESSMENT_RECORDED");
    }

    @Test
    @DisplayName("accepts uploads up to the domain's 10 MiB per file and 11 MiB per request")
    void acceptsUploadsUpToTheDomainsLimit() {
        // when
        long[] limits = {multipartConfig.getMaxFileSize(), multipartConfig.getMaxRequestSize()};

        // then
        assertThat(limits).containsExactly(SupportingDocument.MAX_SIZE_IN_BYTES, 11L * 1024 * 1024);
    }

    @Test
    @DisplayName("builds absolute links with the scheme and host forwarded by the proxy")
    void buildsAbsoluteLinksWithTheSchemeAndHostForwardedByTheProxy() throws IOException, InterruptedException {
        // given
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource"))
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "civicgrant.example")
                .build();

        // when
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, BodyHandlers.ofString());
        }

        // then
        assertThat(response.body()).contains("\"resource\":\"https://civicgrant.example\"");
    }

    private FundingCallId anOpenFundingCall(String reference) {
        FundingCallId id = new FundingCallId(UUID.randomUUID());
        jdbc.sql("""
                        INSERT INTO civicgrant.funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                            total_budget, committed_amount, max_amount_per_application, version, created_at)
                        VALUES (:id, :reference, 'Buurtprojecten', 'Projets de quartier',
                            now() - interval '1 day', now() + interval '30 days', 50000.00, 0.00, 15000.00, 0, now())
                        """).param("id", id.value()).param("reference", reference).update();
        return id;
    }

    private ApplicationId aSubmittedApplication(String callReference, String enterpriseNumber, Actor applicant) {
        ApplicationId id = createApplication.create(new CreateApplicationCommand(
                anOpenFundingCall(callReference),
                aRegisteredOrganisation(enterpriseNumber, "Buurtwerk " + callReference, applicant),
                proposalTitled("Buurtproject " + callReference),
                applicant));
        attachDocument.attach(new AttachDocumentCommand(id, "projectplan.pdf", "application/pdf", PDF, applicant));
        submitApplication.submit(new SubmitApplicationCommand(id, applicant));
        return id;
    }

    private OrganisationId aRegisteredOrganisation(String enterpriseNumber, String name, Actor applicant) {
        return registerOrganisation.register(new RegisterOrganisationCommand(
                new EnterpriseNumber(enterpriseNumber), name, LegalForm.VZW_ASBL, applicant));
    }

    private static ProjectProposal proposalTitled(String title) {
        return new ProjectProposal(
                title,
                "Vier weken activiteiten voor de buurt, met vrijwilligers uit de wijk.",
                euros("8000.00"),
                euros("12000.00"),
                Language.NL);
    }

    private List<String> auditedEventTypesOf(ApplicationId id) {
        return jdbc.sql("SELECT event_type FROM civicgrant.audit_event WHERE grant_application_id = :id")
                .param("id", id.value())
                .query(String.class)
                .list();
    }
}
