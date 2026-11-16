package be.achzak.civicgrant.domain.grantapplication.service;

import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.DocumentDownload;
import be.achzak.civicgrant.domain.grantapplication.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.DownloadDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.out.DocumentStorage;
import be.achzak.civicgrant.domain.grantapplication.port.out.GrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Role;
import java.util.Objects;

public class DownloadDocumentService implements DownloadDocumentUseCase {

    private final GrantApplicationRepository applications;
    private final DocumentStorage documents;

    public DownloadDocumentService(GrantApplicationRepository applications, DocumentStorage documents) {
        this.applications = Objects.requireNonNull(applications, "applications");
        this.documents = Objects.requireNonNull(documents, "documents");
    }

    @Override
    public DocumentDownload download(DownloadDocumentQuery query) {
        Objects.requireNonNull(query, "query");
        query.actor().requireAnyRole(Role.APPLICANT, Role.ASSESSOR, Role.DECISION_MAKER);
        SupportingDocument document =
                applications.getById(query.applicationId()).document(query.documentId(), query.actor());
        return new DocumentDownload(document, documents.open(query.applicationId(), document.id()));
    }
}
