package be.achzak.civicgrant.domain.grantapplication.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.search.ApplicationPage;

public interface SearchApplicationsUseCase {

    ApplicationPage search(SearchApplicationsQuery query);
}
