package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.util.Objects;

public record ApproveApplicationCommand(
        ApplicationId applicationId, Money grantedAmount, String motivation, Actor actor) implements Command {

    public ApproveApplicationCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(grantedAmount, "grantedAmount");
        Objects.requireNonNull(motivation, "motivation");
        Objects.requireNonNull(actor, "actor");
    }
}
