package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;

public interface CreateApplicationUseCase {

    ApplicationId create(CreateApplicationCommand command);
}
