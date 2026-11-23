package be.achzak.civicgrant.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@WiringTest
@DisplayName("Organisation wiring")
class OrganisationWiringTest {

    @Autowired
    private RegisterOrganisationUseCase registerOrganisation;

    @Autowired
    private SearchOrganisationsUseCase searchOrganisations;

    @Autowired
    private JdbcClient jdbc;

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
}
