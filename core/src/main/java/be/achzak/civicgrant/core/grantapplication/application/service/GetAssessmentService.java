package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAssessmentService implements GetAssessmentUseCase {

    private final GrantApplicationRepository applications;

    public GetAssessmentService(GrantApplicationRepository applications) {
        this.applications = Objects.requireNonNull(applications, "applications");
    }

    @Override
    @Transactional(readOnly = true)
    public Assessment get(GetAssessmentQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.ASSESSOR, Role.DECISION_MAKER);
        return applications.getById(query.applicationId()).assessmentFor(query.actor());
    }
}
