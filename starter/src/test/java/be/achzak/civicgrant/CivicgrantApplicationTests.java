package be.achzak.civicgrant;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
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

    @Autowired
    private JdbcClient jdbc;

    @LocalServerPort
    private int port;

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

    @Test
    @DisplayName("builds absolute links with the scheme and host forwarded by the proxy")
    void buildsAbsoluteLinksWithTheSchemeAndHostForwardedByTheProxy() throws IOException, InterruptedException {
        // given
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource"))
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "civicgrant.example")
                .build();

        // when
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, BodyHandlers.ofString());
        }

        // then
        assertThat(response.body()).contains("\"resource\":\"https://civicgrant.example\"");
    }
}
