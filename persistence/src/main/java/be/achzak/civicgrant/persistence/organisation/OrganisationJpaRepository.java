package be.achzak.civicgrant.persistence.organisation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface OrganisationJpaRepository extends Repository<OrganisationJpaEntity, UUID> {

    Optional<OrganisationJpaEntity> findById(UUID id);

    Optional<OrganisationJpaEntity> findByEnterpriseNumber(String enterpriseNumber);
}
