package be.achzak.civicgrant.core.organisation.application.port.out;

import static be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode.ORGANISATION_NOT_FOUND;

import be.achzak.civicgrant.core.common.domain.error.NotFoundException;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.util.Optional;

public interface OrganisationRepository {

    Optional<Organisation> findById(OrganisationId id);

    default Organisation getById(OrganisationId id) {
        return findById(id)
                .orElseThrow(() ->
                        new NotFoundException(ORGANISATION_NOT_FOUND, "Organisation " + id.value() + " not found"));
    }

    Optional<Organisation> findByEnterpriseNumber(EnterpriseNumber enterpriseNumber);

    void save(Organisation organisation);
}
