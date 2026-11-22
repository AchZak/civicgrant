package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;

public interface GetApplicationUseCase {

    GrantApplication get(GetApplicationQuery query);
}
