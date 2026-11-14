package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.Objects;

public class GetApplicationService implements GetApplicationUseCase {

    private final GrantApplicationRepository applications;

    public GetApplicationService(GrantApplicationRepository applications) {
        this.applications = Objects.requireNonNull(applications, "applications");
    }

    @Override
    public GrantApplication get(GetApplicationQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return applications
                .findById(query.applicationId())
                .filter(application -> application.isVisibleTo(query.actor()))
                .orElseThrow(() -> new NotFoundException(
                        "APPLICATION_NOT_FOUND",
                        "Application " + query.applicationId().value() + " not found"));
    }
}
