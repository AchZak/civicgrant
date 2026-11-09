package be.achzak.civicgrant.domain.shared;

import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Localized text")
class LocalizedTextTest {

    @Test
    @DisplayName("requires a Dutch version")
    void requiresDutchVersion() {
        // when
        Throwable thrown = catchThrowable(() -> new LocalizedText(" ", "Projets de quartier"));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("TRANSLATION_MISSING");
    }

    @Test
    @DisplayName("requires a French version")
    void requiresFrenchVersion() {
        // when
        Throwable thrown = catchThrowable(() -> new LocalizedText("Buurtprojecten", ""));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("TRANSLATION_MISSING");
    }
}
