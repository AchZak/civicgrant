package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.OrganisationId;

public interface RegisterOrganisationUseCase {

    OrganisationId register(RegisterOrganisationCommand command);
}
