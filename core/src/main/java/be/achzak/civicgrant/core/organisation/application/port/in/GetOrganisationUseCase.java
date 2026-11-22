package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.Organisation;

public interface GetOrganisationUseCase {

    Organisation get(GetOrganisationQuery query);
}
