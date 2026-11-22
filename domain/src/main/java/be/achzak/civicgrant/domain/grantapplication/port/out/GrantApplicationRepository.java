package be.achzak.civicgrant.domain.grantapplication.port.out;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSearchCriteria;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
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
