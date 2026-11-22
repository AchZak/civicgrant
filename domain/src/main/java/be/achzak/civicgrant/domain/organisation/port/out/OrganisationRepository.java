package be.achzak.civicgrant.domain.organisation.port.out;

import static be.achzak.civicgrant.domain.organisation.model.OrganisationErrorCode.ORGANISATION_NOT_FOUND;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
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
