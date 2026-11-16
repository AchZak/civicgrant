package be.achzak.civicgrant.domain.grantapplication.model;

import java.nio.charset.StandardCharsets;

public final class PdfFiles {

    private static final byte[] PDF_HEADER = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);

    private PdfFiles() {}

    public static byte[] aPdf() {
        return aPdfOf(1024);
    }

    public static byte[] aPdfOf(int sizeInBytes) {
        byte[] content = new byte[sizeInBytes];
        System.arraycopy(PDF_HEADER, 0, content, 0, Math.min(PDF_HEADER.length, sizeInBytes));
        return content;
    }

    public static byte[] aPng() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    }
}
