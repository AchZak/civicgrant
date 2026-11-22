package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.OrganisationId;

public interface RegisterOrganisationUseCase {

    OrganisationId register(RegisterOrganisationCommand command);
}
