package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.common.application.cqs.Command;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;

public record RegisterOrganisationCommand(
        EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Actor actor) implements Command {}
