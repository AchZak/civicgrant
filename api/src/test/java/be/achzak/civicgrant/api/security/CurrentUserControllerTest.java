package be.achzak.civicgrant.api.security;

import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.common.domain.identity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(CurrentUserController.class)
@DisplayName("Current user endpoint")
class CurrentUserControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("returns the caller's id and roles in the order of the contract")
    void returnsTheCallersIdAndRolesInTheOrderOfTheContract() {
        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/me")
                .with(accessToken(Role.DECISION_MAKER, Role.APPLICANT))
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.id")
                .asString()
                .isEqualTo(actor().id().value());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.roles")
                .asArray()
                .containsExactly("APPLICANT", "DECISION_MAKER");
    }

    @Test
    @DisplayName("returns no roles for a caller who holds none")
    void returnsNoRolesForACallerWhoHoldsNone() {
        // when
        MvcTestResult result = mvc.get().uri("/api/v1/me").with(accessToken()).exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.roles").asArray().isEmpty();
    }
}
