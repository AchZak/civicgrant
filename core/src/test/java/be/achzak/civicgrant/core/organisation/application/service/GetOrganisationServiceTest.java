package be.achzak.civicgrant.core.organisation.application.service;

import static be.achzak.civicgrant.core.organisation.domain.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.aProgrammeManager;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.core.organisation.application.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Getting an organisation")
class GetOrganisationServiceTest {

    @Test
    @DisplayName("returns the organisation with this id")
    void returnsTheOrganisationWithThisId() {
        // given
        Organisation organisation = anOrganisation().build();
        GetOrganisationService service = new GetOrganisationService(new InMemoryOrganisationRepository(organisation));

        // when
        Organisation found = service.get(new GetOrganisationQuery(organisation.id(), anApplicant()));

        // then
        assertThat(found).isSameAs(organisation);
    }

    @Test
    @DisplayName("refuses an unknown organisation as not found")
    void refusesUnknownOrganisationAsNotFound() {
        // given
        GetOrganisationService service = new GetOrganisationService(new InMemoryOrganisationRepository());

        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetOrganisationQuery(OrganisationId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("ORGANISATION_NOT_FOUND");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Role.class, names = "PROGRAMME_MANAGER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("lets applicants, assessors and decision makers get an organisation")
    void letsApplicantsAndStaffGetAnOrganisation(Role role) {
        // given
        Organisation organisation = anOrganisation().build();
        GetOrganisationService service = new GetOrganisationService(new InMemoryOrganisationRepository(organisation));

        // when
        Organisation found = service.get(new GetOrganisationQuery(organisation.id(), actor("user-1", role)));

        // then
        assertThat(found).isSameAs(organisation);
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesActorWithoutAnyRole() {
        // given
        Organisation organisation = anOrganisation().build();
        GetOrganisationService service = new GetOrganisationService(new InMemoryOrganisationRepository(organisation));

        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetOrganisationQuery(organisation.id(), anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    @Test
    @DisplayName("refuses a programme manager, who has no business with organisations")
    void refusesProgrammeManager() {
        // given
        Organisation organisation = anOrganisation().build();
        GetOrganisationService service = new GetOrganisationService(new InMemoryOrganisationRepository(organisation));

        // when
        Throwable thrown =
                catchThrowable(() -> service.get(new GetOrganisationQuery(organisation.id(), aProgrammeManager())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
