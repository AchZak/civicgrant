package be.achzak.civicgrant.domain.shared;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LocalizedTextTest {

    @Test
    void requiresDutchVersion() {
        assertThatThrownBy(() -> new LocalizedText(" ", "Projets de quartier"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "TRANSLATION_MISSING");
    }

    @Test
    void requiresFrenchVersion() {
        assertThatThrownBy(() -> new LocalizedText("Buurtprojecten", ""))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "TRANSLATION_MISSING");
    }
}
