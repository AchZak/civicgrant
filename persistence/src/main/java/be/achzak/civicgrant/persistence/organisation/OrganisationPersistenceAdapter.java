package be.achzak.civicgrant.persistence.organisation;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.ConflictException;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
class OrganisationPersistenceAdapter implements OrganisationRepository {

    private static final String UNIQUE_ENTERPRISE_NUMBER = "uq_organisation_enterprise_number";

    private final OrganisationJpaRepository organisations;
    private final EntityManager entityManager;

    OrganisationPersistenceAdapter(OrganisationJpaRepository organisations, EntityManager entityManager) {
        this.organisations = organisations;
        this.entityManager = entityManager;
    }

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
                    "ORGANISATION_ALREADY_REGISTERED",
                    "Enterprise number " + organisation.enterpriseNumber().value() + " is already registered");
        }
    }
}
