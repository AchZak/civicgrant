package be.achzak.civicgrant.core.grantapplication.application.service;

import be.achzak.civicgrant.core.grantapplication.application.port.in.DocumentDownload;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.out.DocumentStorage;
import be.achzak.civicgrant.core.grantapplication.application.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DownloadDocumentService implements DownloadDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final DocumentStorage documents;

    public DownloadDocumentService(GrantApplicationRepository applications, DocumentStorage documents) {
        this.applications = applications;
        this.documents = documents;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownload download(DownloadDocumentQuery query) {
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        SupportingDocument document =
                applications.getById(query.applicationId()).document(query.documentId(), query.actor());
        return new DocumentDownload(document, documents.open(query.applicationId(), document.id()));
    }
}
