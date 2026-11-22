package be.achzak.civicgrant.persistence.grantapplication;

import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationReference;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationVisibility;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationSnapshot;
import be.achzak.civicgrant.core.grantapplication.domain.Language;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Grant application search")
class GrantApplicationSearchTest {

    private static final FundingCallId NEIGHBOURHOOD_CALL =
            new FundingCallId(UUID.fromString("5ea7c400-0001-4000-8000-000000000001"));
    private static final FundingCallId CULTURE_CALL =
            new FundingCallId(UUID.fromString("5ea7c400-0002-4000-8000-000000000002"));
    private static final OrganisationId ORGANISATION_ID =
            new OrganisationId(UUID.fromString("5ea7c400-0003-4000-8000-000000000003"));
    private static final UserId ME = new UserId("applicant-1");
    private static final UserId SOMEONE_ELSE = new UserId("applicant-2");
    private static final ApplicationVisibility AS_APPLICANT = new ApplicationVisibility(ME, false);
    private static final ApplicationVisibility AS_STAFF = new ApplicationVisibility(ME, true);
    private static final Set<ApplicationStatus> ANY_STATUS = Set.of();
    private static final Set<FundingCallId> ANY_CALL = Set.of();
    private static final Instant MONDAY = Instant.parse("2026-04-13T09:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-04-14T09:00:00Z");
    private static final Instant WEDNESDAY = Instant.parse("2026-04-15T09:00:00Z");
    private static final ProjectProposal PROPOSAL = new ProjectProposal(
            "Samen tuinieren in de wijk",
            "Een gedeelde moestuin voor de buurt.",
            euros("8000.00"),
            euros("12000.00"),
            Language.NL);

    @Autowired
    private GrantApplicationRepository applications;

    @Autowired
    private OrganisationRepository organisations;

    @Autowired
    private JdbcClient jdbc;

    private int lastReferenceNumber;

    @BeforeEach
    void storeTheFundingCallsAndOrganisationTheApplicationsReferTo() {
        insertFundingCall(NEIGHBOURHOOD_CALL, "NBH-SEARCH");
        insertFundingCall(CULTURE_CALL, "CULT-SEARCH");
        organisations.save(Organisation.restore(
                ORGANISATION_ID, new EnterpriseNumber("0202239951"), "Buurtwerk vzw", LegalForm.VZW_ASBL, MONDAY));
    }

    @Test
    @DisplayName("shows an applicant only their own applications")
    void showsAnApplicantOnlyTheirOwnApplications() {
        // given
        ApplicationId myDraft = store(ME, ApplicationStatus.DRAFT, MONDAY);
        ApplicationId mySubmitted = store(ME, ApplicationStatus.SUBMITTED, TUESDAY);
        store(SOMEONE_ELSE, ApplicationStatus.DRAFT, WEDNESDAY);
        store(SOMEONE_ELSE, ApplicationStatus.SUBMITTED, WEDNESDAY);

        // when
        ApplicationPage page =
                applications.search(new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, ANY_CALL), 1, 20);

        // then
        assertThat(idsOf(page)).containsExactly(mySubmitted, myDraft);
        assertThat(page.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("shows staff their own draft and everyone's applications once submitted")
    void showsStaffTheirOwnDraftAndEveryonesApplicationsOnceSubmitted() {
        // given
        ApplicationId myDraft = store(ME, ApplicationStatus.DRAFT, MONDAY);
        store(SOMEONE_ELSE, ApplicationStatus.DRAFT, TUESDAY);
        ApplicationId othersSubmitted = store(SOMEONE_ELSE, ApplicationStatus.SUBMITTED, WEDNESDAY);

        // when
        ApplicationPage page =
                applications.search(new ApplicationSearchCriteria(AS_STAFF, ANY_STATUS, ANY_CALL), 1, 20);

        // then
        assertThat(idsOf(page)).containsExactly(othersSubmitted, myDraft);
        assertThat(page.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("keeps only the statuses asked for")
    void keepsOnlyTheStatusesAskedFor() {
        // given
        store(ME, ApplicationStatus.DRAFT, MONDAY);
        ApplicationId submitted = store(ME, ApplicationStatus.SUBMITTED, TUESDAY);

        // when
        ApplicationPage page = applications.search(
                new ApplicationSearchCriteria(AS_APPLICANT, Set.of(ApplicationStatus.SUBMITTED), ANY_CALL), 1, 20);

        // then
        assertThat(idsOf(page)).containsExactly(submitted);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("keeps only the funding calls asked for")
    void keepsOnlyTheFundingCallsAskedFor() {
        // given
        store(ApplicationId.generate(), ME, NEIGHBOURHOOD_CALL, ApplicationStatus.DRAFT, MONDAY);
        ApplicationId toCulture = store(ApplicationId.generate(), ME, CULTURE_CALL, ApplicationStatus.DRAFT, TUESDAY);

        // when
        ApplicationPage page = applications.search(
                new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, Set.of(CULTURE_CALL)), 1, 20);

        // then
        assertThat(idsOf(page)).containsExactly(toCulture);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("maps every field of a summary")
    void mapsEveryFieldOfASummary() {
        // given
        ApplicationId submitted = store(ME, ApplicationStatus.SUBMITTED, TUESDAY);

        // when
        ApplicationPage page =
                applications.search(new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, ANY_CALL), 1, 20);

        // then
        assertThat(page.items())
                .containsExactly(new ApplicationSummary(
                        submitted,
                        new ApplicationReference("CG-2026-000001"),
                        NEIGHBOURHOOD_CALL,
                        ORGANISATION_ID,
                        ApplicationStatus.SUBMITTED,
                        "Samen tuinieren in de wijk",
                        euros("8000.00"),
                        TUESDAY,
                        TUESDAY));
    }

    @Test
    @DisplayName("lists the most recently changed first, and ties by id")
    void listsTheMostRecentlyChangedFirstAndTiesById() {
        // given
        ApplicationId oldest = store(ME, ApplicationStatus.DRAFT, MONDAY);
        // PostgreSQL compares UUIDs as unsigned bytes, unlike UUID.compareTo, so ffffffff-... sorts last.
        ApplicationId tiedHigh =
                store(id("ffffffff-0000-4000-8000-000000000000"), ME, ApplicationStatus.DRAFT, TUESDAY);
        ApplicationId tiedLow = store(id("00000000-0000-4000-8000-000000000000"), ME, ApplicationStatus.DRAFT, TUESDAY);
        ApplicationId newest = store(ME, ApplicationStatus.DRAFT, WEDNESDAY);

        // when
        ApplicationPage page =
                applications.search(new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, ANY_CALL), 1, 20);

        // then
        assertThat(idsOf(page)).containsExactly(newest, tiedLow, tiedHigh, oldest);
    }

    @Test
    @DisplayName("returns the requested page and the total across all pages")
    void returnsTheRequestedPageAndTheTotalAcrossAllPages() {
        // given
        ApplicationId oldest = store(ME, ApplicationStatus.DRAFT, MONDAY);
        store(ME, ApplicationStatus.DRAFT, TUESDAY);
        store(ME, ApplicationStatus.DRAFT, WEDNESDAY);

        // when
        ApplicationPage page =
                applications.search(new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, ANY_CALL), 2, 2);

        // then
        assertThat(idsOf(page)).containsExactly(oldest);
        assertThat(page)
                .extracting(ApplicationPage::page, ApplicationPage::pageSize, ApplicationPage::total)
                .containsExactly(2, 2, 3L);
    }

    @Test
    @DisplayName("returns an empty page however far past the last one, with the total")
    void returnsAnEmptyPageHoweverFarPastTheLastOneWithTheTotal() {
        // given
        store(ME, ApplicationStatus.DRAFT, MONDAY);

        // when
        ApplicationPage page = applications.search(
                new ApplicationSearchCriteria(AS_APPLICANT, ANY_STATUS, ANY_CALL), Integer.MAX_VALUE, 100);

        // then
        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isEqualTo(1);
    }

    private ApplicationId store(UserId applicant, ApplicationStatus status, Instant updatedAt) {
        return store(ApplicationId.generate(), applicant, status, updatedAt);
    }

    private ApplicationId store(ApplicationId id, UserId applicant, ApplicationStatus status, Instant updatedAt) {
        return store(id, applicant, NEIGHBOURHOOD_CALL, status, updatedAt);
    }

    private ApplicationId store(
            ApplicationId id, UserId applicant, FundingCallId call, ApplicationStatus status, Instant updatedAt) {
        lastReferenceNumber++;
        applications.save(GrantApplication.restore(new GrantApplicationSnapshot(
                id,
                ApplicationReference.of(Year.of(2026), lastReferenceNumber),
                call,
                ORGANISATION_ID,
                applicant,
                PROPOSAL,
                List.of(),
                status,
                status == ApplicationStatus.DRAFT ? null : updatedAt,
                null,
                null,
                MONDAY,
                updatedAt,
                0)));
        return id;
    }

    private void insertFundingCall(FundingCallId id, String reference) {
        jdbc.sql("""
                        INSERT INTO civicgrant.funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                            total_budget, committed_amount, max_amount_per_application, version, created_at)
                        VALUES (:id, :reference, 'Buurtprojecten 2026', 'Projets de quartier 2026',
                            '2026-01-15T00:00:00Z', '2026-12-31T23:00:00Z', 50000.00, 0.00, 15000.00, 0,
                            '2026-01-10T09:00:00Z')
                        """).param("id", id.value()).param("reference", reference).update();
    }

    private static ApplicationId id(String uuid) {
        return new ApplicationId(UUID.fromString(uuid));
    }

    private static List<ApplicationId> idsOf(ApplicationPage page) {
        return page.items().stream().map(ApplicationSummary::id).toList();
    }
}
