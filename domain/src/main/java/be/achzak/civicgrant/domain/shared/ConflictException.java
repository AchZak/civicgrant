package be.achzak.civicgrant.domain.shared;

import java.io.Serial;

public final class ConflictException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
