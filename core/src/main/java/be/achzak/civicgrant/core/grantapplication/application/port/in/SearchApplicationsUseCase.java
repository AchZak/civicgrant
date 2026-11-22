package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;

public interface SearchApplicationsUseCase {

    ApplicationPage search(SearchApplicationsQuery query);
}
