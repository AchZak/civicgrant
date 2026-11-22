package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record GetApplicationQuery(ApplicationId applicationId, Actor actor) implements Query {

    public GetApplicationQuery {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(actor, "actor");
    }
}
