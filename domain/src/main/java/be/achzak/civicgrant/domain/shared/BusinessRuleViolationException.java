package be.achzak.civicgrant.domain.shared;

import java.io.Serial;

public final class BusinessRuleViolationException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
