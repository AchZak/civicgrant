package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record EditApplicationCommand(ApplicationId applicationId, ProjectProposal proposal, Actor actor) {

    public EditApplicationCommand {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(actor, "actor");
    }
}
