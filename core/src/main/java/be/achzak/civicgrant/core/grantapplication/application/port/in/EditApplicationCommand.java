package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Command;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;

public record EditApplicationCommand(ApplicationId applicationId, ProjectProposal proposal, Actor actor)
        implements Command {}
