package be.achzak.civicgrant.domain.organisation.port.out;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import java.util.Optional;

public interface OrganisationRepository {

    Optional<Organisation> findById(OrganisationId id);

    Optional<Organisation> findByEnterpriseNumber(EnterpriseNumber enterpriseNumber);

    void save(Organisation organisation);
}
