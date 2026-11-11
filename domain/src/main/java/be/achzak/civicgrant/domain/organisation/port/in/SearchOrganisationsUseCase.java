package be.achzak.civicgrant.domain.organisation.port.in;

import be.achzak.civicgrant.domain.organisation.model.Organisation;
import java.util.List;

public interface SearchOrganisationsUseCase {

    List<Organisation> search(SearchOrganisationsQuery query);
}
