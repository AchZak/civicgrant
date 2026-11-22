package be.achzak.civicgrant.domain.organisation.service;

import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.Actors.aProgrammeManager;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Searching organisations")
class SearchOrganisationsServiceTest {

    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0202239951");

    @Test
    @DisplayName("finds the organisation registered with this enterprise number")
    void findsTheOrganisationRegisteredWithThisEnterpriseNumber() {
        // given
        Organisation organisation =
                anOrganisation().withEnterpriseNumber(ENTERPRISE_NUMBER).build();
        SearchOrganisationsService service = new SearchOrganisationsService(
                new InMemoryOrganisationRepository(anOrganisation().build(), organisation));

        // when
        List<Organisation> found = service.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, anApplicant()));

        // then
        assertThat(found).containsExactly(organisation);
    }

    @Test
    @DisplayName("finds nothing for an enterprise number nobody registered")
    void findsNothingForAnEnterpriseNumberNobodyRegistered() {
        // given
        SearchOrganisationsService service = new SearchOrganisationsService(
                new InMemoryOrganisationRepository(anOrganisation().build()));

        // when
        List<Organisation> found = service.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, anApplicant()));

        // then
        assertThat(found).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Role.class, names = "PROGRAMME_MANAGER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("lets applicants, assessors and decision makers search organisations")
    void letsApplicantsAndStaffSearchOrganisations(Role role) {
        // given
        Organisation organisation =
                anOrganisation().withEnterpriseNumber(ENTERPRISE_NUMBER).build();
        SearchOrganisationsService service =
                new SearchOrganisationsService(new InMemoryOrganisationRepository(organisation));

        // when
        List<Organisation> found =
                service.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, actor("user-1", role)));

        // then
        assertThat(found).containsExactly(organisation);
    }

    @Test
    @DisplayName("refuses an actor without any role")
    void refusesActorWithoutAnyRole() {
        // given
        SearchOrganisationsService service = new SearchOrganisationsService(new InMemoryOrganisationRepository());

        // when
        Throwable thrown = catchThrowable(
                () -> service.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, anActorWithoutRoles())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    @Test
    @DisplayName("refuses a programme manager, who has no business with organisations")
    void refusesProgrammeManager() {
        // given
        SearchOrganisationsService service = new SearchOrganisationsService(new InMemoryOrganisationRepository());

        // when
        Throwable thrown = catchThrowable(
                () -> service.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, aProgrammeManager())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
