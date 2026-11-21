package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Command;
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
