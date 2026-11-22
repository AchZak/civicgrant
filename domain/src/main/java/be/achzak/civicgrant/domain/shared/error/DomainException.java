package be.achzak.civicgrant.domain.shared.error;

import java.io.Serial;
import java.util.Objects;

public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ForbiddenException, BusinessRuleViolationException, ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
