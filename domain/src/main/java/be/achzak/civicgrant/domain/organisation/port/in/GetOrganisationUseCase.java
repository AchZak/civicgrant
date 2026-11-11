package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.Organisation;

public interface GetOrganisationUseCase {

    Organisation get(GetOrganisationQuery query);
}
