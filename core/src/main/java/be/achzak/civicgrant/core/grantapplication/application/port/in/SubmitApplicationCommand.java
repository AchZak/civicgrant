package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Command;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;

public record SubmitApplicationCommand(ApplicationId applicationId, Actor actor) implements Command {}
