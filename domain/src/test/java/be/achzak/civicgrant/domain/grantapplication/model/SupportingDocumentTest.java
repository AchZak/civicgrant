package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
    private static final long TEN_MEBIBYTES = 10L * 1024 * 1024;

    private static SupportingDocument upload(String fileName, String contentType, long sizeInBytes) {
        return SupportingDocument.upload(
                fileName, contentType, sizeInBytes, anApplicant().id(), NOW);
    }

    @Nested
    @DisplayName("Size")
    class Size {

        @Test
        @DisplayName("accepts exactly 10 MiB")
        void acceptsExactlyTenMebibytes() {
            // when
            SupportingDocument document = upload("begroting.pdf", PDF, TEN_MEBIBYTES);

            // then
            assertThat(document.sizeInBytes()).isEqualTo(TEN_MEBIBYTES);
        }

        @Test
        @DisplayName("rejects one byte more than 10 MiB")
        void rejectsOneByteMoreThanTenMebibytes() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", PDF, TEN_MEBIBYTES + 1));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_TOO_LARGE");
        }

        @Test
        @DisplayName("rejects an empty file")
        void rejectsEmptyFile() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", PDF, 0));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_EMPTY");
        }
    }

    @Nested
    @DisplayName("Type")
    class Type {

        @Test
        @DisplayName("rejects anything but a PDF")
        void rejectsAnythingButPdf() {
            // when
            Throwable thrown = catchThrowable(() -> upload("foto.png", "image/png", 1024));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NOT_PDF");
        }

        @Test
        @DisplayName("reads the declared type regardless of case")
        void readsTheDeclaredTypeRegardlessOfCase() {
            // when
            Throwable thrown = catchThrowable(() -> upload("begroting.pdf", "Application/PDF", 1024));

            // then
            assertThat(thrown).isNull();
        }
    }

    @Nested
    @DisplayName("File name")
    class FileName {

        @Test
        @DisplayName("strips whitespace around the file name")
        void stripsWhitespaceAroundTheFileName() {
            // when
            SupportingDocument document = upload("  begroting.pdf ", PDF, 1024);

            // then
            assertThat(document.fileName()).isEqualTo("begroting.pdf");
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"   ", "../begroting.pdf", "map\\begroting.pdf", "begroting\r\n.pdf"})
        @DisplayName("rejects a blank name, a path or a control character")
        void rejectsBlankNamePathOrControlCharacter(String fileName) {
            // when
            Throwable thrown = catchThrowable(() -> upload(fileName, PDF, 1024));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NAME_INVALID");
        }

        @Test
        @DisplayName("rejects a name longer than 255 characters")
        void rejectsNameLongerThan255Characters() {
            // given
            String tooLong = "a".repeat(252) + ".pdf";

            // when
            Throwable thrown = catchThrowable(() -> upload(tooLong, PDF, 1024));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("DOCUMENT_NAME_INVALID");
        }
    }
}
