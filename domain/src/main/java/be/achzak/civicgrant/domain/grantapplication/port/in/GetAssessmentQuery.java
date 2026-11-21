package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Query;
import java.util.Objects;

public record GetAssessmentQuery(ApplicationId applicationId, Actor actor) implements Query {

    public GetAssessmentQuery {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(actor, "actor");
    }
}
