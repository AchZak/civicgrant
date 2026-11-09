package be.achzak.civicgrant.api.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class UserAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<OAuth2Error> errors = new ArrayList<>();
        if (!grantsApiScope(token.getClaim(EntraAccessToken.SCOPES_CLAIM))) {
            errors.add(invalidToken("The token doesn't grant the " + EntraAccessToken.API_SCOPE + " scope"));
        }
        if (!isText(token.getClaim(EntraAccessToken.OBJECT_ID_CLAIM))) {
            errors.add(invalidToken("The token has no object id"));
        }
        return OAuth2TokenValidatorResult.failure(errors);
    }

    // Entra sends scp as one space-separated string, and app-only tokens have no scp at all.
    private static boolean grantsApiScope(Object scopes) {
        return scopes instanceof String value && Arrays.asList(value.split(" ")).contains(EntraAccessToken.API_SCOPE);
    }

    private static boolean isText(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private static OAuth2Error invalidToken(String description) {
        return new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null);
    }
}
