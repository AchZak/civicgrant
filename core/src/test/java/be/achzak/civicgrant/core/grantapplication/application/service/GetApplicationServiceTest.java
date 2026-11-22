package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anAssessor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anotherApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Getting a grant application")
class GetApplicationServiceTest {

    private static final GrantApplication DRAFT = aDraftApplication().build();
    private static final GrantApplication SUBMITTED = aSubmittedApplication().build();

    private final GetApplicationService service =
            new GetApplicationService(new InMemoryGrantApplicationRepository(DRAFT, SUBMITTED));

    @Test
    @DisplayName("gives the applicant their own draft")
    void givesTheApplicantTheirOwnDraft() {
        // when
        GrantApplication found = service.get(new GetApplicationQuery(DRAFT.id(), anApplicant()));

        // then
        assertThat(found).isSameAs(DRAFT);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ASSESSOR", "DECISION_MAKER"})
    @DisplayName("gives staff a submitted application")
    void givesStaffASubmittedApplication(Role role) {
        // when
        GrantApplication found = service.get(new GetApplicationQuery(SUBMITTED.id(), actor("staff-1", role)));

        // then
        assertThat(found).isSameAs(SUBMITTED);
    }

    @Test
    @DisplayName("hides a draft from staff, as if it didn't exist")
    void hidesADraftFromStaff() {
        // when
        Throwable thrown = catchThrowable(() -> service.get(new GetApplicationQuery(DRAFT.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("hides an application from another applicant, as if it didn't exist")
    void hidesAnApplicationFromAnotherApplicant() {
        // when
        Throwable thrown = catchThrowable(() -> service.get(new GetApplicationQuery(DRAFT.id(), anotherApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetApplicationQuery(ApplicationId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesAnActorWithoutAnyRole() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetApplicationQuery(DRAFT.id(), anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
