package be.achzak.civicgrant.domain.shared.error;

import java.io.Serial;

public final class NotFoundException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
