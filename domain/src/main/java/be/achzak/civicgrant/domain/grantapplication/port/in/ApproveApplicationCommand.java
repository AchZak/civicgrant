package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Command;
import be.achzak.civicgrant.domain.shared.Money;
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
