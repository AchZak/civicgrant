package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record SubmitApplicationCommand(ApplicationId applicationId, Actor actor) {

    public SubmitApplicationCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(actor, "actor");
    }
}
