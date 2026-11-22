package be.achzak.civicgrant.domain.shared;

import be.achzak.civicgrant.domain.shared.error.ErrorCode;

public enum SharedErrorCode implements ErrorCode {
    AMOUNT_NEGATIVE,
    AMOUNT_TOO_PRECISE,
    CONCURRENT_UPDATE,
    GRANTED_AMOUNT_NOT_POSITIVE,
    ROLE_REQUIRED,
    TRANSLATION_MISSING
}
