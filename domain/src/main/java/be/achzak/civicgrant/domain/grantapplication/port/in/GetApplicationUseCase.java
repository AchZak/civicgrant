package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;

public interface GetApplicationUseCase {

    GrantApplication get(GetApplicationQuery query);
}
