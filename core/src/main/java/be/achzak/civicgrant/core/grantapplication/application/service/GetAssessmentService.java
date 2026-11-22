package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.util.Objects;

public class GetAssessmentService implements GetAssessmentUseCase {

    private final GrantApplicationRepository applications;

    public GetAssessmentService(GrantApplicationRepository applications) {
        this.applications = Objects.requireNonNull(applications, "applications");
    }

    @Override
    public Assessment get(GetAssessmentQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.ASSESSOR, Role.DECISION_MAKER);
        return applications.getById(query.applicationId()).assessmentFor(query.actor());
    }
}
