package be.achzak.civicgrant.core.organisation.application.port.out;

import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryOrganisationRepository implements OrganisationRepository {

    private final Map<OrganisationId, Organisation> organisations = new LinkedHashMap<>();

    public InMemoryOrganisationRepository(Organisation... organisations) {
        for (Organisation organisation : organisations) {
            save(organisation);
        }
    }

    @Override
    public Optional<Organisation> findById(OrganisationId id) {
        return Optional.ofNullable(organisations.get(id));
    }

    @Override
    public Optional<Organisation> findByEnterpriseNumber(EnterpriseNumber enterpriseNumber) {
        return organisations.values().stream()
                .filter(organisation -> organisation.enterpriseNumber().equals(enterpriseNumber))
                .findFirst();
    }

    @Override
    public void save(Organisation organisation) {
        organisations.put(organisation.id(), organisation);
    }
}
