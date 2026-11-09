package be.achzak.civicgrant.domain.shared;

import java.io.Serial;

public final class ForbiddenException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
