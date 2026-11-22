package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Searching grant applications")
class SearchApplicationsServiceTest {

    private static final Set<ApplicationStatus> ANY_STATUS = Set.of();
    private static final Set<FundingCallId> ANY_CALL = Set.of();
    private static final FundingCall CULTURE_CALL =
            aFundingCall().withReference("CULT-2026").build();
    private static final GrantApplication MY_DRAFT = aDraftApplication().build();
    private static final GrantApplication MY_SUBMITTED = aSubmittedApplication().build();
    private static final GrantApplication OTHERS_DRAFT =
            aDraftApplication().withApplicant(anotherApplicant().id()).build();
    private static final GrantApplication OTHERS_ASSESSED = anAssessedApplication()
            .withApplicant(anotherApplicant().id())
            .forCall(CULTURE_CALL)
            .build();

    private final SearchApplicationsService service = new SearchApplicationsService(
            new InMemoryGrantApplicationRepository(MY_DRAFT, MY_SUBMITTED, OTHERS_DRAFT, OTHERS_ASSESSED));

    @Test
    @DisplayName("gives an applicant their own applications, most recently changed first")
    void givesAnApplicantTheirOwnApplicationsMostRecentlyChangedFirst() {
        // when
        ApplicationPage page = service.search(new SearchApplicationsQuery(ANY_STATUS, ANY_CALL, 1, 20, anApplicant()));

        // then
        assertThat(idsOf(page)).containsExactly(MY_SUBMITTED.id(), MY_DRAFT.id());
        assertThat(page.total()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ASSESSOR", "DECISION_MAKER"})
    @DisplayName("gives staff every application except other people's drafts")
    void givesStaffEveryApplicationExceptOtherPeoplesDrafts(Role role) {
        // given
        Actor staff = actor("staff-1", role);

        // when
        ApplicationPage page = service.search(new SearchApplicationsQuery(ANY_STATUS, ANY_CALL, 1, 20, staff));

        // then
        assertThat(idsOf(page)).containsExactly(OTHERS_ASSESSED.id(), MY_SUBMITTED.id());
        assertThat(page.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("keeps only the statuses asked for")
    void keepsOnlyTheStatusesAskedFor() {
        // given
        SearchApplicationsQuery query =
                new SearchApplicationsQuery(Set.of(ApplicationStatus.DRAFT), ANY_CALL, 1, 20, anApplicant());

        // when
        ApplicationPage page = service.search(query);

        // then
        assertThat(idsOf(page)).containsExactly(MY_DRAFT.id());
    }

    @Test
    @DisplayName("keeps only the funding calls asked for")
    void keepsOnlyTheFundingCallsAskedFor() {
        // given
        SearchApplicationsQuery query = new SearchApplicationsQuery(
                ANY_STATUS, Set.of(CULTURE_CALL.id()), 1, 20, actor("staff-1", Role.ASSESSOR));

        // when
        ApplicationPage page = service.search(query);

        // then
        assertThat(idsOf(page)).containsExactly(OTHERS_ASSESSED.id());
    }

    @Test
    @DisplayName("returns the page asked for, with the total across all pages")
    void returnsThePageAskedForWithTheTotalAcrossAllPages() {
        // when
        ApplicationPage page = service.search(new SearchApplicationsQuery(ANY_STATUS, ANY_CALL, 2, 1, anApplicant()));

        // then
        assertThat(idsOf(page)).containsExactly(MY_DRAFT.id());
        assertThat(page)
                .extracting(ApplicationPage::page, ApplicationPage::pageSize, ApplicationPage::total)
                .containsExactly(2, 1, 2L);
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesAnActorWithoutAnyRole() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.search(new SearchApplicationsQuery(ANY_STATUS, ANY_CALL, 1, 20, anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    private static List<ApplicationId> idsOf(ApplicationPage page) {
        return page.items().stream().map(ApplicationSummary::id).toList();
    }
}
