package be.achzak.civicgrant.api.security;

import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.ApiWebMvcTest.FRONTEND_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.common.domain.identity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(CurrentUserController.class)
@DisplayName("Cross-origin requests")
class CrossOriginRequestsTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("lets the frontend send a token without signing in first")
    void letsTheFrontendSendATokenWithoutSigningInFirst() {
        // when
        MvcTestResult result = mvc.options()
                .uri("/api/v1/me")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization")
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("authorization");
    }

    @Test
    @DisplayName("refuses a preflight from another origin")
    void refusesAPreflightFromAnotherOrigin() {
        // when
        MvcTestResult result = mvc.options()
                .uri("/api/v1/me")
                .header(HttpHeaders.ORIGIN, "https://elsewhere.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).headers().doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    @DisplayName("lets the frontend read the answer to a call")
    void letsTheFrontendReadTheAnswerToACall() {
        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/me")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .with(accessToken(Role.APPLICANT))
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN);
    }
}
