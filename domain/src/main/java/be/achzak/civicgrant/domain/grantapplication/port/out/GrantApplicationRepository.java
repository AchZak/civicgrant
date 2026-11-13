package be.achzak.civicgrant.domain.grantapplication.port.out;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import java.util.Optional;

public interface GrantApplicationRepository {

    Optional<GrantApplication> findById(ApplicationId id);

    void save(GrantApplication application);
}
