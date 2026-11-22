package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.sharedkernel.application.Query;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.util.Objects;

public record SearchOrganisationsQuery(EnterpriseNumber enterpriseNumber, Actor actor) implements Query {

    public SearchOrganisationsQuery {
        Objects.requireNonNull(enterpriseNumber, "enterpriseNumber");
        Objects.requireNonNull(actor, "actor");
    }
}
