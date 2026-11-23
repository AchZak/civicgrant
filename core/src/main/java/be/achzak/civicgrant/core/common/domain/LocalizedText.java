package be.achzak.civicgrant.core.common.domain;

import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.TRANSLATION_MISSING;

import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import java.util.Objects;

public record LocalizedText(String nl, String fr) {

    public LocalizedText {
        Objects.requireNonNull(nl, "nl");
        Objects.requireNonNull(fr, "fr");
        if (nl.isBlank() || fr.isBlank()) {
            throw new BusinessRuleViolationException(
                    TRANSLATION_MISSING, "Text needs both a Dutch and a French version");
        }
    }
}
