package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;

public record RegisterOrganisationCommand(
        EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Actor actor) implements Command {}
