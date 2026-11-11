package be.achzak.civicgrant.persistence.organisation;

import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;

final class OrganisationJpaMapper {

    private OrganisationJpaMapper() {}

    static Organisation toDomain(OrganisationJpaEntity entity) {
        return Organisation.restore(
                new OrganisationId(entity.getId()),
                new EnterpriseNumber(entity.getEnterpriseNumber()),
                entity.getName(),
                entity.getLegalForm(),
                entity.getCreatedAt());
    }

    static OrganisationJpaEntity toEntity(Organisation organisation) {
        return new OrganisationJpaEntity(
                organisation.id().value(),
                organisation.enterpriseNumber().value(),
                organisation.name(),
                organisation.legalForm(),
                organisation.createdAt());
    }
}
