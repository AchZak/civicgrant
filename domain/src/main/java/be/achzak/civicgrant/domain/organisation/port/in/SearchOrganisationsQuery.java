package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Query;
import java.util.Objects;

public record SearchOrganisationsQuery(EnterpriseNumber enterpriseNumber, Actor actor) implements Query {

    public SearchOrganisationsQuery {
        Objects.requireNonNull(enterpriseNumber, "enterpriseNumber");
        Objects.requireNonNull(actor, "actor");
    }
}
