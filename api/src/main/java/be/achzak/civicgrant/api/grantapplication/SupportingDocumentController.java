package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.SupportingDocumentsApi;
import be.achzak.civicgrant.api.generated.model.SupportingDocumentDto;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.DocumentDownload;
import be.achzak.civicgrant.domain.grantapplication.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.DownloadDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetDocumentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetDocumentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class SupportingDocumentController implements SupportingDocumentsApi {

    private final AttachDocumentUseCase attachDocument;
    private final GetDocumentUseCase getDocument;
    private final DownloadDocumentUseCase downloadDocument;
    private final RemoveDocumentUseCase removeDocument;
    private final CurrentActor currentActor;

    SupportingDocumentController(
            AttachDocumentUseCase attachDocument,
            GetDocumentUseCase getDocument,
            DownloadDocumentUseCase downloadDocument,
            RemoveDocumentUseCase removeDocument,
            CurrentActor currentActor) {
        this.attachDocument = attachDocument;
        this.getDocument = getDocument;
        this.downloadDocument = downloadDocument;
        this.removeDocument = removeDocument;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<SupportingDocumentDto> attachDocument(UUID applicationId, MultipartFile file) {
        Actor actor = currentActor.get();
        DocumentId id = attachDocument.attach(SupportingDocumentApiMapper.toCommand(applicationId, file, actor));
        return ResponseEntity.created(locationOf(id)).body(read(applicationId, id, actor));
    }

    @Override
    public ResponseEntity<SupportingDocumentDto> getDocument(UUID applicationId, UUID documentId) {
        return ResponseEntity.ok(read(applicationId, new DocumentId(documentId), currentActor.get()));
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(UUID applicationId, UUID documentId) {
        DocumentDownload download = downloadDocument.download(new DownloadDocumentQuery(
                new ApplicationId(applicationId), new DocumentId(documentId), currentActor.get()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(download.document().sizeInBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        attachmentNamed(download.document().fileName()))
                .body(new InputStreamResource(download.content()));
    }

    @Override
    public ResponseEntity<Void> removeDocument(UUID applicationId, UUID documentId) {
        removeDocument.remove(new RemoveDocumentCommand(
                new ApplicationId(applicationId), new DocumentId(documentId), currentActor.get()));
        return ResponseEntity.noContent().build();
    }

    private SupportingDocumentDto read(UUID applicationId, DocumentId documentId, Actor actor) {
        return SupportingDocumentApiMapper.toDto(
                getDocument.get(new GetDocumentQuery(new ApplicationId(applicationId), documentId, actor)));
    }

    private static URI locationOf(DocumentId id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{documentId}")
                .buildAndExpand(id.value())
                .toUri();
    }

    private static String attachmentNamed(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
