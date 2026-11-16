package be.achzak.civicgrant.domain.grantapplication.port.in;

public interface DownloadDocumentUseCase {

    DocumentDownload download(DownloadDocumentQuery query);
}
