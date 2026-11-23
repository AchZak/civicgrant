package be.achzak.civicgrant.persistence.organisation;

import static be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode.ORGANISATION_ALREADY_REGISTERED;

import be.achzak.civicgrant.core.common.domain.error.ConflictException;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrganisationPersistenceAdapter implements OrganisationRepository {

    private static final String UNIQUE_ENTERPRISE_NUMBER = "uq_organisation_enterprise_number";

    private final OrganisationJpaRepository organisations;
    private final EntityManager entityManager;

    @Override
    public Optional<Organisation> findById(OrganisationId id) {
        return organisations.findById(id.value()).map(OrganisationJpaMapper::toDomain);
    }

    @Override
    public Optional<Organisation> findByEnterpriseNumber(EnterpriseNumber enterpriseNumber) {
        return organisations.findByEnterpriseNumber(enterpriseNumber.value()).map(OrganisationJpaMapper::toDomain);
    }

    @Override
    public void save(Organisation organisation) {
        try {
            entityManager.persist(OrganisationJpaMapper.toEntity(organisation));
            // Flushed now, so a duplicate registered concurrently fails here, where it can become a conflict.
            entityManager.flush();
        } catch (ConstraintViolationException e) {
            if (!UNIQUE_ENTERPRISE_NUMBER.equals(e.getConstraintName())) {
                throw e;
            }
            throw new ConflictException(
                    ORGANISATION_ALREADY_REGISTERED,
                    "Enterprise number " + organisation.enterpriseNumber().value() + " is already registered");
        }
    }
}
