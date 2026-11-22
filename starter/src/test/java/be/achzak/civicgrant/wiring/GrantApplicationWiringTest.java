package be.achzak.civicgrant.wiring;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.wiring.WiringFixtures.DECISION_MAKER;
import static be.achzak.civicgrant.wiring.WiringFixtures.PDF;
import static be.achzak.civicgrant.wiring.WiringFixtures.proposalTitled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.DocumentStorage;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@WiringTest
@DisplayName("Grant application wiring")
class GrantApplicationWiringTest {

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
    private ApproveApplicationUseCase approveApplication;

    @Autowired
    private RejectApplicationUseCase rejectApplication;

    @Autowired
    private DocumentStorage documents;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private WiringFixtures fixtures;

    @Nested
    @DisplayName("Drafting an application")
    class Drafting {

        @Test
        @DisplayName("creates a numbered application and writes its audit entry")
        void createsANumberedApplicationAndWritesItsAuditEntry() {
            // given
            Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
            FundingCallId fundingCall = fixtures.anOpenFundingCall("NBH-CREATE");
            OrganisationId organisation =
                    fixtures.aRegisteredOrganisation("0476543281", "Speelpleinwerking Zonnebloem", applicant);

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
                    fixtures.anOpenFundingCall("NBH-EDIT"),
                    fixtures.aRegisteredOrganisation("0428123455", "Repaircafe De Schroef", applicant),
                    proposalTitled("Repaircafe in het buurthuis"),
                    applicant));

            // when
            editApplication.edit(
                    new EditApplicationCommand(id, proposalTitled("Repaircafe met fietsherstel"), applicant));

            // then
            assertThat(jdbc.sql("SELECT project_title FROM civicgrant.grant_application WHERE id = :id")
                            .param("id", id.value())
                            .query(String.class)
                            .single())
                    .isEqualTo("Repaircafe met fietsherstel");
            assertThat(auditedEventTypesOf(id)).containsExactlyInAnyOrder("APPLICATION_CREATED", "APPLICATION_EDITED");
        }
    }

    @Nested
    @DisplayName("Attaching and removing documents")
    class SupportingDocuments {

        @Test
        @DisplayName("stores an attached document with its file and writes its audit entry")
        void storesAnAttachedDocumentWithItsFileAndWritesItsAuditEntry() {
            // given
            Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
            ApplicationId id = createApplication.create(new CreateApplicationCommand(
                    fixtures.anOpenFundingCall("NBH-ATTACH"),
                    fixtures.aRegisteredOrganisation("0431000001", "Buurtmoestuin De Kiem", applicant),
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
                    fixtures.anOpenFundingCall("NBH-REMOVE"),
                    fixtures.aRegisteredOrganisation("0432000089", "Fietsatelier De Spaak", applicant),
                    proposalTitled("Fietsatelier voor de buurt"),
                    applicant));
            DocumentId document = attachDocument.attach(
                    new AttachDocumentCommand(id, "offerte.pdf", "application/pdf", PDF, applicant));

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
    }

    @Nested
    @DisplayName("Submitting an application")
    class Submitting {

        @Test
        @DisplayName("stores a submitted application and writes its audit entry")
        void storesASubmittedApplicationAndWritesItsAuditEntry() {
            // given
            Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
            ApplicationId id = createApplication.create(new CreateApplicationCommand(
                    fixtures.anOpenFundingCall("NBH-SUBMIT"),
                    fixtures.aRegisteredOrganisation("0433000080", "Buurtkeuken De Pollepel", applicant),
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
    }

    @Nested
    @DisplayName("Assessing an application")
    class Assessing {

        @Test
        @DisplayName("stores an assessment and writes its audit entry")
        void storesAnAssessmentAndWritesItsAuditEntry() {
            // given
            Actor applicant = new Actor(new UserId("applicant-1"), Set.of(Role.APPLICANT));
            Actor assessor = new Actor(new UserId("assessor-1"), Set.of(Role.ASSESSOR));
            ApplicationId id = fixtures.aSubmittedApplication("NBH-ASSESS", "0433000179", applicant);

            // when
            recordAssessment.record(new RecordAssessmentCommand(
                    id, true, new Recommendation.Approve(euros("7500.00")), "Sterk buurtproject.", assessor));

            // then
            assertThat(jdbc.sql("""
                            SELECT a.status || ' ' || s.assessor_oid || ' ' || s.recommendation || ' '
                                || s.proposed_amount
                            FROM civicgrant.grant_application a
                            JOIN civicgrant.assessment s ON s.grant_application_id = a.id
                            WHERE a.id = :id
                            """).param("id", id.value()).query(String.class).single())
                    .isEqualTo("ASSESSED assessor-1 APPROVE 7500.00");
            assertThat(auditedEventTypesOf(id))
                    .containsExactlyInAnyOrder(
                            "APPLICATION_CREATED", "DOCUMENT_ATTACHED", "APPLICATION_SUBMITTED", "ASSESSMENT_RECORDED");
        }
    }

    @Nested
    @DisplayName("Deciding on an application")
    class Deciding {

        @Test
        @DisplayName("stores an approval, commits its grant to the call's budget and writes its audit entry")
        void storesAnApprovalCommitsItsGrantAndWritesItsAuditEntry() {
            // given
            ApplicationId id = fixtures.anAssessedApplication("NBH-APPROVE", "0433000278");

            // when
            approveApplication.approve(new ApproveApplicationCommand(
                    id, euros("7000.00"), "Het project versterkt de buurt.", DECISION_MAKER));

            // then
            assertThat(jdbc.sql("""
                            SELECT a.status || ' ' || d.decision_maker_oid || ' ' || d.outcome || ' '
                                || d.granted_amount || ' ' || f.committed_amount
                            FROM civicgrant.grant_application a
                            JOIN civicgrant.decision d ON d.grant_application_id = a.id
                            JOIN civicgrant.funding_call f ON f.id = a.funding_call_id
                            WHERE a.id = :id
                            """).param("id", id.value()).query(String.class).single())
                    .isEqualTo("APPROVED decision-maker-1 APPROVED 7000.00 7000.00");
            assertThat(auditedEventTypesOf(id))
                    .containsExactlyInAnyOrder(
                            "APPLICATION_CREATED",
                            "DOCUMENT_ATTACHED",
                            "APPLICATION_SUBMITTED",
                            "ASSESSMENT_RECORDED",
                            "APPLICATION_APPROVED");
        }

        @Test
        @DisplayName("stores a rejection and writes its audit entry")
        void storesARejectionAndWritesItsAuditEntry() {
            // given
            ApplicationId id = fixtures.anAssessedApplication("NBH-REJECT", "0433000377");

            // when
            rejectApplication.reject(new RejectApplicationCommand(
                    id,
                    RejectionGround.INSUFFICIENT_QUALITY,
                    "Het projectplan is onvoldoende uitgewerkt.",
                    DECISION_MAKER));

            // then
            assertThat(jdbc.sql("""
                            SELECT a.status || ' ' || d.decision_maker_oid || ' ' || d.outcome || ' '
                                || d.rejection_ground
                            FROM civicgrant.grant_application a
                            JOIN civicgrant.decision d ON d.grant_application_id = a.id
                            WHERE a.id = :id
                            """).param("id", id.value()).query(String.class).single())
                    .isEqualTo("REJECTED decision-maker-1 REJECTED INSUFFICIENT_QUALITY");
            assertThat(auditedEventTypesOf(id))
                    .containsExactlyInAnyOrder(
                            "APPLICATION_CREATED",
                            "DOCUMENT_ATTACHED",
                            "APPLICATION_SUBMITTED",
                            "ASSESSMENT_RECORDED",
                            "APPLICATION_REJECTED");
        }
    }

    private List<String> auditedEventTypesOf(ApplicationId id) {
        return jdbc.sql("SELECT event_type FROM civicgrant.audit_event WHERE grant_application_id = :id")
                .param("id", id.value())
                .query(String.class)
                .list();
    }
}
