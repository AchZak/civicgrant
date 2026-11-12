package be.achzak.civicgrant.api.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.util.Arrays;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

// jwt() bypasses the app's converter and validators, so the token carries what they would have produced.
public final class AccessTokens {

    private static final String USER_ID = "3f1c2b7e-5a4d-4c3b-9e2f-1a0b9c8d7e6f";

    private AccessTokens() {}

    public static JwtRequestPostProcessor accessToken(Role... roles) {
        return jwt().jwt(token -> token.claim(EntraAccessToken.OBJECT_ID_CLAIM, USER_ID))
                .authorities(Arrays.stream(roles)
                        .map(role -> new SimpleGrantedAuthority(EntraAccessToken.ROLE_AUTHORITY_PREFIX + role.name()))
                        .toArray(GrantedAuthority[]::new));
    }

    public static Actor actor(Role... roles) {
        return new Actor(new UserId(USER_ID), Set.of(roles));
    }
}
