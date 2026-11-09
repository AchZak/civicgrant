package be.achzak.civicgrant.api.security;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class CurrentActor {

    public Actor get() {
        if (!(SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken authentication)) {
            throw new IllegalStateException("The request isn't authenticated with an Entra access token");
        }
        String objectId = authentication.getToken().getClaimAsString(EntraAccessToken.OBJECT_ID_CLAIM);
        return new Actor(new UserId(Objects.requireNonNull(objectId, "oid claim")), roles(authentication));
    }

    // Roles the domain doesn't know are ignored, so a new app role in Entra can't break sign-in.
    private static Set<Role> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith(EntraAccessToken.ROLE_AUTHORITY_PREFIX))
                .map(authority -> authority.substring(EntraAccessToken.ROLE_AUTHORITY_PREFIX.length()))
                .flatMap(CurrentActor::toRole)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<Role> toRole(String name) {
        return Arrays.stream(Role.values()).filter(role -> role.name().equals(name));
    }
}
