package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.error.NotFoundException;
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
        return applications
                .findById(query.applicationId())
                .filter(application -> application.isVisibleTo(query.actor()))
                .orElseThrow(() -> new NotFoundException(
                        APPLICATION_NOT_FOUND,
                        "Application " + query.applicationId().value() + " not found"));
    }
}
