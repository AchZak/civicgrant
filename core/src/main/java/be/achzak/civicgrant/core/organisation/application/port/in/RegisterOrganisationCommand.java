package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record RegisterOrganisationCommand(
        EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Actor actor) implements Command {

    public RegisterOrganisationCommand {
        Objects.requireNonNull(enterpriseNumber, "enterpriseNumber");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(legalForm, "legalForm");
        Objects.requireNonNull(actor, "actor");
    }
}
