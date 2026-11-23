package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record GetAssessmentQuery(ApplicationId applicationId, Actor actor) implements Query {}
