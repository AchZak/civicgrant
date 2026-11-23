package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Command;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;

public record CreateApplicationCommand(
        FundingCallId fundingCallId, OrganisationId organisationId, ProjectProposal proposal, Actor actor)
        implements Command {}
