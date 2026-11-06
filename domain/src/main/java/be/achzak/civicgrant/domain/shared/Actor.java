package be.achzak.civicgrant.domain.shared;

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
}
