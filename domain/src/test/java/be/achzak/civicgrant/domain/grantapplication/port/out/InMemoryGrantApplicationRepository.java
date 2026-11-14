package be.achzak.civicgrant.domain.grantapplication.port.out;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryGrantApplicationRepository implements GrantApplicationRepository {

    private final Map<ApplicationId, GrantApplication> applications = new LinkedHashMap<>();

    public InMemoryGrantApplicationRepository(GrantApplication... applications) {
        for (GrantApplication application : applications) {
            this.applications.put(application.id(), application);
        }
    }

    @Override
    public Optional<GrantApplication> findById(ApplicationId id) {
        return Optional.ofNullable(applications.get(id));
    }

    @Override
    public void save(GrantApplication application) {
        applications.put(application.id(), application);
    }
}
