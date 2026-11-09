package be.achzak.civicgrant.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiWebMvcTest(BearerTokenAuthenticationTest.ActorProbe.class)
@Import(BearerTokenAuthenticationTest.ActorProbe.class)
@DisplayName("Bearer token authentication")
class BearerTokenAuthenticationTest {

    private static final String OBJECT_ID = "3f2b8c1e-7d4a-4e9b-a6c5-0d1e2f3a4b5c";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("Rejecting a request")
    class Rejecting {

        @Test
        @DisplayName("answers 401 with a bearer challenge when the request has no token")
        void answers401WithoutAToken() {
            // when
            MvcTestResult result = mvc.get().uri(ActorProbe.PATH).exchange();

            // then
            assertThat(result)
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .headers()
                    .hasHeaderSatisfying(
                            HttpHeaders.WWW_AUTHENTICATE,
                            values -> assertThat(values)
                                    .singleElement()
                                    .asString()
                                    .startsWith("Bearer "));
        }

        @Test
        @DisplayName("answers 401 with invalid_token when the token can't be decoded")
        void answers401ForAnInvalidToken() {
            // given
            given(jwtDecoder.decode("forged")).willThrow(new BadJwtException("Signed by an unknown key"));

            // when
            MvcTestResult result = mvc.get()
                    .uri(ActorProbe.PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer forged")
                    .exchange();

            // then
            assertThat(result)
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .headers()
                    .hasHeaderSatisfying(
                            HttpHeaders.WWW_AUTHENTICATE,
                            values -> assertThat(values)
                                    .singleElement()
                                    .asString()
                                    .contains("error=\"invalid_token\""));
        }
    }

    @Nested
    @DisplayName("Building the actor")
    class BuildingTheActor {

        @Test
        @DisplayName("takes the user id from the object id and the roles from the app roles")
        void takesTheIdAndRolesFromTheToken() {
            // given
            given(jwtDecoder.decode("valid")).willReturn(token(List.of("ASSESSOR", "DECISION_MAKER")));

            // when
            MvcTestResult result = mvc.get()
                    .uri(ActorProbe.PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
                    .exchange();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.id").asString().isEqualTo(OBJECT_ID);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.roles")
                    .asArray()
                    .containsExactlyInAnyOrder("ASSESSOR", "DECISION_MAKER");
        }

        @Test
        @DisplayName("ignores app roles the domain doesn't know")
        void ignoresUnknownRoles() {
            // given
            given(jwtDecoder.decode("valid")).willReturn(token(List.of("APPLICANT", "AUDITOR")));

            // when
            MvcTestResult result = mvc.get()
                    .uri(ActorProbe.PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
                    .exchange();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.roles").asArray().containsExactly("APPLICANT");
        }
    }

    private static Jwt token(List<String> appRoles) {
        return Jwt.withTokenValue("valid")
                .header("alg", "RS256")
                .claim(EntraAccessToken.OBJECT_ID_CLAIM, OBJECT_ID)
                .claim(EntraAccessToken.SCOPES_CLAIM, EntraAccessToken.API_SCOPE)
                .claim(EntraAccessToken.APP_ROLES_CLAIM, appRoles)
                .build();
    }

    @RestController
    static class ActorProbe {

        static final String PATH = "/actor-probe";

        private final CurrentActor currentActor;

        ActorProbe(CurrentActor currentActor) {
            this.currentActor = currentActor;
        }

        @GetMapping(PATH)
        ActorView actor() {
            Actor actor = currentActor.get();
            return new ActorView(actor.id().value(), actor.roles());
        }

        record ActorView(String id, Set<Role> roles) {}
    }
}
