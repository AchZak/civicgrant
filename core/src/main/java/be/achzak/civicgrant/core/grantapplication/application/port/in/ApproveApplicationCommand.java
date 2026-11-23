package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;

public record ApproveApplicationCommand(
        ApplicationId applicationId, Money grantedAmount, String motivation, Actor actor) implements Command {}
