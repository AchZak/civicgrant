package be.achzak.civicgrant.domain.shared;

import java.io.Serial;

public final class NotFoundException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
