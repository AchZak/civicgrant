package be.achzak.civicgrant.core.grantapplication.domain;

import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.util.Objects;

public record ApplicationVisibility(UserId user, boolean staff) {

    public ApplicationVisibility {
        Objects.requireNonNull(user, "user");
    }

    public static ApplicationVisibility of(Actor actor) {
        Objects.requireNonNull(actor, "actor");
        return new ApplicationVisibility(
                actor.id(), actor.hasRole(Role.ASSESSOR) || actor.hasRole(Role.DECISION_MAKER));
    }

    public boolean allows(UserId applicant, ApplicationStatus status) {
        Objects.requireNonNull(applicant, "applicant");
        Objects.requireNonNull(status, "status");
        return user.equals(applicant) || (staff && status != ApplicationStatus.DRAFT);
    }
}
