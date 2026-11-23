package be.achzak.civicgrant.core.grantapplication.application.port.in;

import be.achzak.civicgrant.core.grantapplication.domain.SupportingDocument;
import java.io.InputStream;

public record DocumentDownload(SupportingDocument document, InputStream content) {}
