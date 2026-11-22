package be.achzak.civicgrant.core.sharedkernel.domain.error;

import java.io.Serial;

public final class ConflictException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
