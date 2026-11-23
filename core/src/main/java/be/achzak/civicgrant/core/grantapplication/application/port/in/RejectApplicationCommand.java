package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record RejectApplicationCommand(
        ApplicationId applicationId, RejectionGround ground, String motivation, Actor actor) implements Command {}
