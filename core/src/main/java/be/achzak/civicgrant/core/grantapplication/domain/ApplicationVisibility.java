package be.achzak.civicgrant.core.grantapplication.domain;

import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import java.util.Objects;

public record ApplicationVisibility(UserId user, boolean staff) {

    public ApplicationVisibility {
        Objects.requireNonNull(user, "user");
    }

    public static ApplicationVisibility of(Actor actor) {
        return new ApplicationVisibility(
                actor.id(), actor.hasRole(Role.ASSESSOR) || actor.hasRole(Role.DECISION_MAKER));
    }

    public boolean allows(UserId applicant, ApplicationStatus status) {
        return user.equals(applicant) || (staff && status != ApplicationStatus.DRAFT);
    }
}
