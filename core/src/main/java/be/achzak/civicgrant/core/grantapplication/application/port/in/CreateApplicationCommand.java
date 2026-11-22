package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record CreateApplicationCommand(
        FundingCallId fundingCallId, OrganisationId organisationId, ProjectProposal proposal, Actor actor)
        implements Command {

    public CreateApplicationCommand {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(actor, "actor");
    }
}
