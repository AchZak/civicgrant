package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
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
