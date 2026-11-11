package be.achzak.civicgrant;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test/v2.0",
            "spring.security.oauth2.resourceserver.jwt.audiences=civicgrant-api-test"
        })
@DisplayName("CivicGrant application")
class CivicgrantApplicationTests {

    @Autowired
    private RegisterOrganisationUseCase registerOrganisation;

    @Autowired
    private SearchOrganisationsUseCase searchOrganisations;

    @Test
    @DisplayName("starts the full context against PostgreSQL")
    void contextLoads() {}

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
}
