package be.achzak.civicgrant.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("User access token validator")
class UserAccessTokenValidatorTest {

    private static final String OBJECT_ID = "3f2b8c1e-7d4a-4e9b-a6c5-0d1e2f3a4b5c";

    private final UserAccessTokenValidator validator = new UserAccessTokenValidator();

    @Test
    @DisplayName("accepts a user token that grants access_as_user among other scopes")
    void acceptsAUserTokenForThisApi() {
        // given
        Jwt token = token(Map.of("scp", "profile access_as_user", "oid", OBJECT_ID));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("rejects an app-only token, which has no scopes")
    void rejectsAnAppOnlyToken() {
        // given
        Jwt token = token(Map.of("oid", OBJECT_ID, "roles", List.of("ASSESSOR")));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a token that grants other scopes only")
    void rejectsATokenForAnotherScope() {
        // given
        Jwt token = token(Map.of("scp", "User.Read", "oid", OBJECT_ID));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a scope claim that isn't a string")
    void rejectsAScopeClaimThatIsNotAString() {
        // given
        Jwt token = token(Map.of("scp", List.of("access_as_user"), "oid", OBJECT_ID));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a token without an object id")
    void rejectsATokenWithoutAnObjectId() {
        // given
        Jwt token = token(Map.of("scp", "access_as_user"));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    private static Jwt token(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(existing -> existing.putAll(claims))
                .build();
    }
}
