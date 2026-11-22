package be.achzak.civicgrant.core.sharedkernel.domain.error;

import java.io.Serial;

public final class BusinessRuleViolationException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleViolationException(ErrorCode code, String message) {
        super(code, message);
    }
}
