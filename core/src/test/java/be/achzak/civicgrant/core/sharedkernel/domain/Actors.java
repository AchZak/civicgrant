package be.achzak.civicgrant.core.sharedkernel.domain;

import java.util.Set;

public final class Actors {

    private Actors() {}

    public static Actor anApplicant() {
        return actor("applicant-1", Role.APPLICANT);
    }

    public static Actor anotherApplicant() {
        return actor("applicant-2", Role.APPLICANT);
    }

    public static Actor anAssessor() {
        return actor("assessor-1", Role.ASSESSOR);
    }

    public static Actor aDecisionMaker() {
        return actor("decision-maker-1", Role.DECISION_MAKER);
    }

    public static Actor aProgrammeManager() {
        return actor("programme-manager-1", Role.PROGRAMME_MANAGER);
    }

    public static Actor anActorWithoutRoles() {
        return actor("no-role-1");
    }

    public static Actor actor(String id, Role... roles) {
        return new Actor(new UserId(id), Set.of(roles));
    }
}
