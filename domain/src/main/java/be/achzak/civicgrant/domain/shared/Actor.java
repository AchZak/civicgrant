package be.achzak.civicgrant.domain.shared;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record Actor(UserId id, Set<Role> roles) {

    public Actor {
        Objects.requireNonNull(id, "id");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public void requireAnyRole(Role role, Role... otherRoles) {
        Set<Role> accepted = EnumSet.of(role, otherRoles);
        if (Collections.disjoint(roles, accepted)) {
            throw new ForbiddenException("ROLE_REQUIRED", "User " + id.value() + " has none of the roles " + accepted);
        }
    }
}
