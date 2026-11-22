package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.GetDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.util.Objects;

public class GetDocumentService implements GetDocumentUseCase {

    private final GrantApplicationRepository applications;

    public GetDocumentService(GrantApplicationRepository applications) {
        this.applications = Objects.requireNonNull(applications, "applications");
    }

    @Override
    public SupportingDocument get(GetDocumentQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        return applications.getById(query.applicationId()).document(query.documentId(), query.actor());
    }
}
