package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;

final class RequiredText {

    private RequiredText() {}

    static String strip(String text, int maxLength, String requiredCode, String tooLongCode) {
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
