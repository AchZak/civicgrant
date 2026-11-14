package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record CreateApplicationCommand(
        FundingCallId fundingCallId, OrganisationId organisationId, ProjectProposal proposal, Actor actor) {

    public CreateApplicationCommand {
        Objects.requireNonNull(fundingCallId, "fundingCallId");
        Objects.requireNonNull(organisationId, "organisationId");
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(actor, "actor");
    }
}
