package be.achzak.civicgrant.core.grantapplication.application.port.in;

import static be.achzak.civicgrant.core.common.domain.identity.Actors.anApplicant;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdf;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdfOf;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attach document command")
class AttachDocumentCommandTest {

    private static final ApplicationId APPLICATION = ApplicationId.generate();

    private static AttachDocumentCommand attaching(byte[] content) {
        return new AttachDocumentCommand(APPLICATION, "begroting.pdf", "application/pdf", content, anApplicant());
    }

    @Test
    @DisplayName("equals another command with the same bytes in another array")
    void equalsAnotherCommandWithTheSameBytesInAnotherArray() {
        // when
        AttachDocumentCommand first = attaching(aPdf());
        AttachDocumentCommand second = attaching(aPdf());

        // then
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("differs from a command with other bytes")
    void differsFromACommandWithOtherBytes() {
        // when
        AttachDocumentCommand first = attaching(aPdfOf(1024));
        AttachDocumentCommand second = attaching(aPdfOf(2048));

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("shows the size of the content instead of its bytes")
    void showsTheSizeOfTheContentInsteadOfItsBytes() {
        // when
        String shown = attaching(aPdfOf(1024)).toString();

        // then
        assertThat(shown).contains("content=1024 bytes").doesNotContain("[B@");
    }
}
