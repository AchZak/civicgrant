package be.achzak.civicgrant.persistence.grantapplication;

import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationSnapshot;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
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
    private static final ProjectProposal EDITED_PROPOSAL = new ProjectProposal(
            "Jardin de quartier",
            "Un potager partagé, avec un compost collectif.",
            euros("9000.00"),
            euros("12000.00"),
            Language.FR);

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

    private static GrantApplication draft(ProjectProposal proposal, Instant updatedAt, long version) {
        return application(proposal, ApplicationStatus.DRAFT, null, updatedAt, version);
    }

    private static GrantApplication application(
            ProjectProposal proposal, ApplicationStatus status, Instant submittedAt, Instant updatedAt, long version) {
        return GrantApplication.restore(new GrantApplicationSnapshot(
                APPLICATION_ID,
                new ApplicationReference("CG-2026-000001"),
                FUNDING_CALL_ID,
                ORGANISATION_ID,
                new UserId("applicant-1"),
                proposal,
                List.of(),
                status,
                submittedAt,
                null,
                null,
                CREATED_AT,
                updatedAt,
                version));
    }
}
