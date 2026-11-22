package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record EditApplicationCommand(ApplicationId applicationId, ProjectProposal proposal, Actor actor)
        implements Command {

    public EditApplicationCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(actor, "actor");
    }
}
