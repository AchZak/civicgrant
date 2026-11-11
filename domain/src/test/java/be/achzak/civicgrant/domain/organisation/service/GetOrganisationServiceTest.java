package be.achzak.civicgrant.domain.organisation.service;

import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.domain.organisation.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.domain.shared.Role;
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
    @EnumSource(Role.class)
    @DisplayName("lets every role get an organisation")
    void letsEveryRoleGetAnOrganisation(Role role) {
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
}
