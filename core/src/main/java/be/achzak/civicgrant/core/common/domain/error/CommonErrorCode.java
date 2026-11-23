package be.achzak.civicgrant.core.common.domain.error;

public enum CommonErrorCode implements ErrorCode {
    AMOUNT_NEGATIVE,
    AMOUNT_TOO_PRECISE,
    CONCURRENT_UPDATE,
    GRANTED_AMOUNT_NOT_POSITIVE,
    ROLE_REQUIRED,
    TRANSLATION_MISSING
}
