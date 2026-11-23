package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record RecordAssessmentCommand(
        ApplicationId applicationId, boolean eligible, Recommendation recommendation, String comment, Actor actor)
        implements Command {}
