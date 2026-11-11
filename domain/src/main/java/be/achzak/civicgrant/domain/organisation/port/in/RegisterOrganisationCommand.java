package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.Objects;

public record RegisterOrganisationCommand(
        EnterpriseNumber enterpriseNumber, String name, LegalForm legalForm, Actor actor) {

    public RegisterOrganisationCommand {
        Objects.requireNonNull(enterpriseNumber, "enterpriseNumber");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(legalForm, "legalForm");
        Objects.requireNonNull(actor, "actor");
    }
}
