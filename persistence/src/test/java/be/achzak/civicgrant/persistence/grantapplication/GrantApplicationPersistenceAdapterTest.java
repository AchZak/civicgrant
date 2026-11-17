package be.achzak.civicgrant.persistence.grantapplication;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Assessment;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationSnapshot;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.UserId;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Grant application persistence")
class GrantApplicationPersistenceAdapterTest {

    private static final ApplicationId APPLICATION_ID =
            new ApplicationId(UUID.fromString("5a7e2c10-0001-4000-8000-000000000001"));
    private static final FundingCallId FUNDING_CALL_ID =
            new FundingCallId(UUID.fromString("5a7e2c10-0002-4000-8000-000000000002"));
    private static final OrganisationId ORGANISATION_ID =
            new OrganisationId(UUID.fromString("5a7e2c10-0003-4000-8000-000000000003"));
    private static final Instant CREATED_AT = Instant.parse("2026-04-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-04-16T14:30:00Z");
    private static final ProjectProposal PROPOSAL = new ProjectProposal(
            "Jardin de quartier",
            "Un potager partagé pour le quartier.",
            euros("8000.00"),
            euros("12000.00"),
            Language.FR);
    private static final SupportingDocument BUDGET = new SupportingDocument(
            new DocumentId(UUID.fromString("5a7e2c10-0004-4000-8000-000000000004")),
            "begroting.pdf",
            245_760,
            new UserId("applicant-1"),
            CREATED_AT);
    private static final SupportingDocument STATUTES = new SupportingDocument(
            new DocumentId(UUID.fromString("5a7e2c10-0005-4000-8000-000000000005")),
            "statuten.pdf",
            102_400,
            new UserId("applicant-1"),
            LATER);
    private static final ProjectProposal EDITED_PROPOSAL = new ProjectProposal(
            "Jardin de quartier",
            "Un potager partagé, avec un compost collectif.",
            euros("9000.00"),
            euros("12000.00"),
            Language.FR);
    private static final Instant ASSESSED_AT = Instant.parse("2026-04-20T09:15:00Z");
    private static final Assessment APPROVAL_RECOMMENDED = new Assessment(
            new UserId("assessor-1"),
            true,
            new Recommendation.Approve(euros("7500.00")),
            "Sterk buurtproject met een realistische begroting.",
            ASSESSED_AT);
    private static final Assessment REJECTION_RECOMMENDED =
            new Assessment(new UserId("assessor-1"), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT);

    @Autowired
    private GrantApplicationRepository applications;

    @Autowired
    private OrganisationRepository organisations;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void storeTheFundingCallAndOrganisationItRefersTo() {
        jdbc.sql("""
                        INSERT INTO civicgrant.funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                            total_budget, committed_amount, max_amount_per_application, version, created_at)
                        VALUES (:id, 'NBH-2026', 'Buurtprojecten 2026', 'Projets de quartier 2026',
                            '2026-01-15T00:00:00Z', '2026-12-31T23:00:00Z', 50000.00, 0.00, 15000.00, 0,
                            '2026-01-10T09:00:00Z')
                        """).param("id", FUNDING_CALL_ID.value()).update();
        organisations.save(Organisation.restore(
                ORGANISATION_ID, new EnterpriseNumber("0202239951"), "Buurtwerk vzw", LegalForm.VZW_ASBL, CREATED_AT));
    }

    @Test
    @DisplayName("saves a new application and loads it back with every column")
    void savesANewApplicationAndLoadsItBackWithEveryColumn() {
        // given
        GrantApplication application = draft(PROPOSAL, CREATED_AT, 0);
        applications.save(application);
        entityManager.clear();

        // when
        Optional<GrantApplication> loaded = applications.findById(APPLICATION_ID);

        // then
        assertThat(loaded).get().usingRecursiveComparison().isEqualTo(application);
    }

    @Test
    @DisplayName("updates a changed application in place and increments its version")
    void updatesAChangedApplicationInPlaceAndIncrementsItsVersion() {
        // given
        applications.save(draft(PROPOSAL, CREATED_AT, 0));
        entityManager.clear();
        GrantApplication submitted = application(EDITED_PROPOSAL, ApplicationStatus.SUBMITTED, LATER, LATER, 0);

        // when
        applications.save(submitted);

        // then
        entityManager.clear();
        assertThat(applications.findById(APPLICATION_ID))
                .get()
                .extracting(
                        GrantApplication::proposal,
                        GrantApplication::status,
                        GrantApplication::submittedAt,
                        GrantApplication::version)
                .containsExactly(EDITED_PROPOSAL, ApplicationStatus.SUBMITTED, Optional.of(LATER), 1L);
    }

    @Test
    @DisplayName("finds nothing for an unknown id")
    void findsNothingForAnUnknownId() {
        // when
        Optional<GrantApplication> loaded = applications.findById(ApplicationId.generate());

        // then
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("refuses to overwrite a change someone else saved since the application was read")
    void refusesToOverwriteAChangeSomeoneElseSaved() {
        // given
        applications.save(draft(PROPOSAL, CREATED_AT, 0));
        jdbc.sql("UPDATE civicgrant.grant_application SET version = version + 1 WHERE id = :id")
                .param("id", APPLICATION_ID.value())
                .update();

        // when
        Throwable thrown = catchThrowable(() -> applications.save(draft(EDITED_PROPOSAL, LATER, 0)));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        conflict -> assertThat(conflict.code()).isEqualTo("CONCURRENT_UPDATE"));
    }

    @Test
    @DisplayName("refuses an application read before its last change was saved")
    void refusesAnApplicationReadBeforeItsLastChangeWasSaved() {
        // given
        applications.save(draft(PROPOSAL, CREATED_AT, 0));
        applications.save(draft(EDITED_PROPOSAL, LATER, 0));

        // when
        Throwable thrown = catchThrowable(() -> applications.save(draft(PROPOSAL, LATER, 0)));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        conflict -> assertThat(conflict.code()).isEqualTo("CONCURRENT_UPDATE"));
    }

    @Test
    @DisplayName("saves the documents of an application and loads them back in upload order")
    void savesTheDocumentsOfAnApplicationAndLoadsThemBack() {
        // given
        GrantApplication application = draftWith(List.of(STATUTES, BUDGET), 0);
        applications.save(application);
        entityManager.clear();

        // when
        Optional<GrantApplication> loaded = applications.findById(APPLICATION_ID);

        // then
        assertThat(loaded).get().extracting(GrantApplication::documents).isEqualTo(List.of(BUDGET, STATUTES));
    }

    @Test
    @DisplayName("stores each document under a blob name made of ids only, as a PDF")
    void storesEachDocumentUnderABlobNameMadeOfIdsOnlyAsAPdf() {
        // when
        applications.save(draftWith(List.of(BUDGET), 0));

        // then
        List<String> stored = jdbc.sql("""
                        SELECT blob_name || ' ' || content_type FROM civicgrant.supporting_document
                        WHERE grant_application_id = :id
                        """)
                .param("id", APPLICATION_ID.value())
                .query(String.class)
                .list();
        assertThat(stored)
                .containsExactly("applications/" + APPLICATION_ID.value() + "/"
                        + BUDGET.id().value() + ".pdf application/pdf");
    }

    @Test
    @DisplayName("deletes the row of a document removed from the application")
    void deletesTheRowOfADocumentRemovedFromTheApplication() {
        // given
        applications.save(draftWith(List.of(BUDGET, STATUTES), 0));
        entityManager.clear();

        // when
        applications.save(draftWith(List.of(BUDGET), 0));

        // then
        entityManager.clear();
        assertThat(applications.findById(APPLICATION_ID))
                .get()
                .extracting(GrantApplication::documents)
                .isEqualTo(List.of(BUDGET));
        assertThat(jdbc.sql("SELECT count(*) FROM civicgrant.supporting_document WHERE grant_application_id = :id")
                        .param("id", APPLICATION_ID.value())
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
    }

    @Nested
    @DisplayName("Storing an assessment")
    class StoringAnAssessment {

        @Test
        @DisplayName("saves an application assessed for approval and loads it back with every column")
        void savesAnApplicationAssessedForApprovalAndLoadsItBack() {
            // given
            GrantApplication application = assessed(APPROVAL_RECOMMENDED, 0);
            applications.save(application);
            entityManager.clear();

            // when
            Optional<GrantApplication> loaded = applications.findById(APPLICATION_ID);

            // then
            assertThat(loaded).get().usingRecursiveComparison().isEqualTo(application);
        }

        @Test
        @DisplayName("loads a recommendation to reject without a proposed amount")
        void loadsARecommendationToRejectWithoutAProposedAmount() {
            // given
            applications.save(assessed(REJECTION_RECOMMENDED, 0));
            entityManager.clear();

            // when
            Optional<GrantApplication> loaded = applications.findById(APPLICATION_ID);

            // then
            assertThat(loaded)
                    .get()
                    .extracting(GrantApplication::assessment)
                    .isEqualTo(Optional.of(REJECTION_RECOMMENDED));
        }

        @Test
        @DisplayName("adds the assessment when a stored application is assessed")
        void addsTheAssessmentWhenAStoredApplicationIsAssessed() {
            // given
            applications.save(submitted());
            entityManager.clear();

            // when
            applications.save(assessed(APPROVAL_RECOMMENDED, 0));

            // then
            entityManager.clear();
            assertThat(applications.findById(APPLICATION_ID))
                    .get()
                    .extracting(GrantApplication::status, GrantApplication::assessment, GrantApplication::version)
                    .containsExactly(ApplicationStatus.ASSESSED, Optional.of(APPROVAL_RECOMMENDED), 1L);
        }

        @Test
        @DisplayName("refuses a blank comment")
        void refusesABlankComment() {
            // given
            applications.save(submitted());

            // when
            Throwable thrown = catchThrowable(() -> insertRejectionWithComment("   "));

            // then
            assertThat(thrown)
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_assessment_comment");
        }

        @Test
        @DisplayName("refuses a second assessment of the same application")
        void refusesASecondAssessmentOfTheSameApplication() {
            // given
            applications.save(assessed(APPROVAL_RECOMMENDED, 0));

            // when
            Throwable thrown = catchThrowable(() -> insertRejectionWithComment("Toch niet."));

            // then
            assertThat(thrown)
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("assessment_pkey");
        }

        private void insertRejectionWithComment(String comment) {
            jdbc.sql("""
                            INSERT INTO civicgrant.assessment (grant_application_id, assessor_oid, eligible,
                                recommendation, proposed_amount, comment, assessed_at)
                            VALUES (:id, 'assessor-2', false, 'REJECT', NULL, :comment, :assessedAt)
                            """)
                    .param("id", APPLICATION_ID.value())
                    .param("comment", comment)
                    .param("assessedAt", ASSESSED_AT.atOffset(ZoneOffset.UTC))
                    .update();
        }
    }

    private static GrantApplication draft(ProjectProposal proposal, Instant updatedAt, long version) {
        return application(proposal, ApplicationStatus.DRAFT, null, updatedAt, version, List.of(), null);
    }

    private static GrantApplication draftWith(List<SupportingDocument> documents, long version) {
        return application(PROPOSAL, ApplicationStatus.DRAFT, null, LATER, version, documents, null);
    }

    private static GrantApplication submitted() {
        return application(PROPOSAL, ApplicationStatus.SUBMITTED, LATER, LATER, 0, List.of(BUDGET), null);
    }

    private static GrantApplication assessed(Assessment assessment, long version) {
        return application(
                PROPOSAL, ApplicationStatus.ASSESSED, LATER, ASSESSED_AT, version, List.of(BUDGET), assessment);
    }

    private static GrantApplication application(
            ProjectProposal proposal, ApplicationStatus status, Instant submittedAt, Instant updatedAt, long version) {
        return application(proposal, status, submittedAt, updatedAt, version, List.of(), null);
    }

    private static GrantApplication application(
            ProjectProposal proposal,
            ApplicationStatus status,
            Instant submittedAt,
            Instant updatedAt,
            long version,
            List<SupportingDocument> documents,
            Assessment assessment) {
        return GrantApplication.restore(new GrantApplicationSnapshot(
                APPLICATION_ID,
                new ApplicationReference("CG-2026-000001"),
                FUNDING_CALL_ID,
                ORGANISATION_ID,
                new UserId("applicant-1"),
                proposal,
                documents,
                status,
                submittedAt,
                assessment,
                null,
                CREATED_AT,
                updatedAt,
                version));
    }
}
