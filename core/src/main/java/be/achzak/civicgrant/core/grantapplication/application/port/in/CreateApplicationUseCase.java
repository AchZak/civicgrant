package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;

public interface CreateApplicationUseCase {

    ApplicationId create(CreateApplicationCommand command);
}
