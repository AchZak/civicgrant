package be.achzak.civicgrant.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(thrown)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "TRANSLATION_MISSING");
    }

    @Test
    @DisplayName("requires a French version")
    void requiresFrenchVersion() {
        // when
        Throwable thrown = catchThrowable(() -> new LocalizedText("Buurtprojecten", ""));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "TRANSLATION_MISSING");
    }
}
