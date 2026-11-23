package be.achzak.civicgrant.core.common.domain.error;

import java.io.Serial;

public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ForbiddenException, BusinessRuleViolationException, ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
