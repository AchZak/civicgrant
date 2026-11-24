package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetApplicationService implements GetApplicationUseCase {

    private final GrantApplicationRepository applications;

    @Override
    @Transactional(readOnly = true)
    public GrantApplication get(GetApplicationQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        GrantApplication application = applications.getById(query.applicationId());
        application.ensureVisibleTo(query.actor());
        return application;
    }
}
