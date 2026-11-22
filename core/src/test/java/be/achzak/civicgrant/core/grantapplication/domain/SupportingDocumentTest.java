package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdf;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdfOf;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPng;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Supporting document")
class SupportingDocumentTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final String PDF = "application/pdf";
    private static final int TEN_MEBIBYTES = 10 * 1024 * 1024;

    private static SupportingDocument upload(String fileName, String contentType, byte[] content) {
        return SupportingDocument.upload(
                fileName, contentType, content, anApplicant().id(), NOW);
    }

    @Nested
    @DisplayName("Size")
    class Size {

        @Test
        @DisplayName("accepts exactly 10 MiB")
        void acceptsExactlyTenMebibytes() {
            // when
            SupportingDocument document = upload("begroting.pdf", PDF, aPdfOf(TEN_MEBIBYTES));

            // then
            assertThat(document.sizeInBytes()).isEqualTo(TEN_MEBIBYTES);
        }

        @Test
        @DisplayName("rejects one byte more than 10 MiB")
        void rejectsOneByteMoreThanTenMebibytes() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", PDF, aPdfOf(TEN_MEBIBYTES + 1)));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_TOO_LARGE");
        }

        @Test
        @DisplayName("rejects an empty file")
        void rejectsEmptyFile() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", PDF, new byte[0]));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_EMPTY");
        }
    }

    @Nested
    @DisplayName("Type")
    class Type {

        @Test
        @DisplayName("rejects a file declared as anything but a PDF")
        void rejectsFileDeclaredAsAnythingButPdf() {
            // when
            Throwable thrown = catchThrowable(() -> upload("foto.png", "image/png", aPdf()));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        }

        @Test
        @DisplayName("reads the declared type regardless of case")
        void readsTheDeclaredTypeRegardlessOfCase() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", "Application/PDF", aPdf()));

            // then
            assertThat(thrown).isNull();
        }

        @Test
        @DisplayName("rejects a file declared as a PDF whose content isn't one")
        void rejectsFileDeclaredAsPdfWhoseContentIsNotOne() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", PDF, aPng()));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        }

        @Test
        @DisplayName("rejects a file shorter than the PDF signature")
        void rejectsFileShorterThanThePdfSignature() {
            // when
            Throwable thrown =
                    catchThrowable(() -> upload("begroting.pdf", PDF, "%PDF".getBytes(StandardCharsets.US_ASCII)));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        }
    }

    @Nested
    @DisplayName("File name")
    class FileName {

        @Test
        @DisplayName("strips whitespace around the file name")
        void stripsWhitespaceAroundTheFileName() {
            // when
            SupportingDocument document = upload("  begroting.pdf ", PDF, aPdf());

            // then
            assertThat(document.fileName()).isEqualTo("begroting.pdf");
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"   ", "../begroting.pdf", "map\\begroting.pdf", "begroting\r\n.pdf"})
        @DisplayName("rejects a blank name, a path or a control character")
        void rejectsBlankNamePathOrControlCharacter(String fileName) {
            // when
            Throwable thrown = catchThrowable(() -> upload(fileName, PDF, aPdf()));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NAME_INVALID");
        }

        @Test
        @DisplayName("rejects a name longer than 255 characters")
        void rejectsNameLongerThan255Characters() {
            // given
            String tooLong = "a".repeat(252) + ".pdf";

            // when
            Throwable thrown = catchThrowable(() -> upload(tooLong, PDF, aPdf()));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NAME_INVALID");
        }
    }
}
