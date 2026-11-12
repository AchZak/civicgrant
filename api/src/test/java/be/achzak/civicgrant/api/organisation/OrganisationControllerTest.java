package be.achzak.civicgrant.api.organisation;

import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.domain.organisation.port.in.GetOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.domain.organisation.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(OrganisationController.class)
@DisplayName("Organisation endpoints")
class OrganisationControllerTest {

    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0202239951");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegisterOrganisationUseCase registerOrganisation;

    @MockitoBean
    private GetOrganisationUseCase getOrganisation;

    @MockitoBean
    private SearchOrganisationsUseCase searchOrganisations;

    @Test
    @DisplayName("registers an organisation and answers 201 with its location and the stored organisation")
    void registersAnOrganisationAndAnswers201WithItsLocationAndTheStoredOrganisation() {
        // given
        Organisation organisation = organisation();
        given(registerOrganisation.register(new RegisterOrganisationCommand(
                        ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, actor(Role.APPLICANT))))
                .willReturn(organisation.id());
        given(getOrganisation.get(new GetOrganisationQuery(organisation.id(), actor(Role.APPLICANT))))
                .willReturn(organisation);

        // when
        MvcTestResult result = register("BE 0202.239.951");

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result)
                .headers()
                .hasValue(
                        "Location",
                        "http://localhost/api/v1/organisations/"
                                + organisation.id().value());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.id")
                .asString()
                .isEqualTo(organisation.id().value().toString());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.enterpriseNumber")
                .asString()
                .isEqualTo("0202239951");
    }

    @Test
    @DisplayName("returns 400 enterpriseNumberInvalidChecksum for a mistyped enterprise number")
    void returns400ForAMistypedEnterpriseNumber() {
        // when
        MvcTestResult result = register("0123456789");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .asString()
                .isEqualTo("urn:problem-type:civicgrant:enterpriseNumberInvalidChecksum");
        then(registerOrganisation).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("returns 409 organisationAlreadyRegistered for an enterprise number already registered")
    void returns409ForAnEnterpriseNumberAlreadyRegistered() {
        // given
        given(registerOrganisation.register(new RegisterOrganisationCommand(
                        ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, actor(Role.APPLICANT))))
                .willThrow(new ConflictException(
                        "ORGANISATION_ALREADY_REGISTERED", "Enterprise number 0202239951 is already registered"));

        // when
        MvcTestResult result = register("0202239951");

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .asString()
                .isEqualTo("urn:problem-type:civicgrant:organisationAlreadyRegistered");
    }

    @Test
    @DisplayName("finds the organisation registered with an enterprise number written with dots")
    void findsTheOrganisationRegisteredWithAnEnterpriseNumberWrittenWithDots() {
        // given
        Organisation organisation = organisation();
        given(searchOrganisations.search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, actor(Role.ASSESSOR))))
                .willReturn(List.of(organisation));

        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/organisations")
                .param("enterpriseNumber", "0202.239.951")
                .with(accessToken(Role.ASSESSOR))
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].id")
                .asString()
                .isEqualTo(organisation.id().value().toString());
    }

    @Test
    @DisplayName("returns an organisation by its id")
    void returnsAnOrganisationByItsId() {
        // given
        Organisation organisation = organisation();
        given(getOrganisation.get(new GetOrganisationQuery(organisation.id(), actor(Role.DECISION_MAKER))))
                .willReturn(organisation);

        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/organisations/{organisationId}", organisation.id().value())
                .with(accessToken(Role.DECISION_MAKER))
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").asString().isEqualTo("Buurtwerk vzw");
        assertThat(result).bodyJson().extractingPath("$.legalForm").asString().isEqualTo("VZW_ASBL");
    }

    private MvcTestResult register(String enterpriseNumber) {
        return mvc.post()
                .uri("/api/v1/organisations")
                .with(accessToken(Role.APPLICANT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"enterpriseNumber": "%s", "name": "Buurtwerk vzw", "legalForm": "VZW_ASBL"}
                        """.formatted(enterpriseNumber))
                .exchange();
    }

    private static Organisation organisation() {
        return Organisation.restore(
                OrganisationId.generate(),
                ENTERPRISE_NUMBER,
                "Buurtwerk vzw",
                LegalForm.VZW_ASBL,
                Instant.parse("2026-01-10T09:00:00Z"));
    }
}
