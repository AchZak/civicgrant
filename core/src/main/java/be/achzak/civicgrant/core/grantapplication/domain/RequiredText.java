package be.achzak.civicgrant.core.grantapplication.domain;

import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.error.ErrorCode;

final class RequiredText {

    private RequiredText() {}

    static String strip(String text, int maxLength, ErrorCode requiredCode, ErrorCode tooLongCode) {
        String stripped = text.strip();
        if (stripped.isEmpty()) {
            throw new BusinessRuleViolationException(requiredCode, "Text is blank");
        }
        if (stripped.length() > maxLength) {
            throw new BusinessRuleViolationException(tooLongCode, "Text exceeds " + maxLength + " characters");
        }
        return stripped;
    }
}
