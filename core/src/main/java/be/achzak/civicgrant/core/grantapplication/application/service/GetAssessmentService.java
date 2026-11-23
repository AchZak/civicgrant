package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAssessmentService implements GetAssessmentUseCase {

    private final GrantApplicationRepository applications;

    @Override
    @Transactional(readOnly = true)
    public Assessment get(GetAssessmentQuery query) {
        query.actor().requireAnyRole(Role.ASSESSOR, Role.DECISION_MAKER);
        return applications.getById(query.applicationId()).assessmentFor(query.actor());
    }
}
