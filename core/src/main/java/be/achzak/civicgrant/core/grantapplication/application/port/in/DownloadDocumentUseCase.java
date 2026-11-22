package be.achzak.civicgrant.core.grantapplication.application.port.in;

public interface DownloadDocumentUseCase {

    DocumentDownload download(DownloadDocumentQuery query);
}
