package be.achzak.civicgrant.domain.shared;

import java.io.Serial;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, BusinessRuleViolationException, ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z][A-Z0-9]*+(?:_[A-Z0-9]++)*+");

    private final String code;

    protected DomainException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        Objects.requireNonNull(code, "code");
        if (!CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("Error code must be UPPER_SNAKE_CASE: " + code);
        }
        this.code = code;
    }

    public String code() {
        return code;
    }
}
