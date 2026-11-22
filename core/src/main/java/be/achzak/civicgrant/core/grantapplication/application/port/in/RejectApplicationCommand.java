package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record RejectApplicationCommand(
        ApplicationId applicationId, RejectionGround ground, String motivation, Actor actor) implements Command {

    public RejectApplicationCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(ground, "ground");
        Objects.requireNonNull(motivation, "motivation");
        Objects.requireNonNull(actor, "actor");
    }
}
