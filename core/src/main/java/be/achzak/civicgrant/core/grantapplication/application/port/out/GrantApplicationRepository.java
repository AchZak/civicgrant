package be.achzak.civicgrant.core.grantapplication.application.port.out;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;

import be.achzak.civicgrant.core.common.domain.error.NotFoundException;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSearchCriteria;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import java.util.Optional;

public interface GrantApplicationRepository {

    Optional<GrantApplication> findById(ApplicationId id);

    default GrantApplication getById(ApplicationId id) {
        return findById(id)
                .orElseThrow(
                        () -> new NotFoundException(APPLICATION_NOT_FOUND, "Application " + id.value() + " not found"));
    }

    void save(GrantApplication application);

    // Most recently changed first, then by id, so pages never overlap.
    ApplicationPage search(ApplicationSearchCriteria criteria, int page, int pageSize);
}
