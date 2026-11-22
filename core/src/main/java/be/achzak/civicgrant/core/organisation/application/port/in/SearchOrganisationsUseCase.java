package be.achzak.civicgrant.core.organisation.application.port.in;

import be.achzak.civicgrant.core.organisation.domain.Organisation;
import java.util.List;

public interface SearchOrganisationsUseCase {

    List<Organisation> search(SearchOrganisationsQuery query);
}
