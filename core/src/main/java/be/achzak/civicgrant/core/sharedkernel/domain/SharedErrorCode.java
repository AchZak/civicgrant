package be.achzak.civicgrant.core.sharedkernel.domain;

import be.achzak.civicgrant.core.sharedkernel.domain.error.ErrorCode;

public enum SharedErrorCode implements ErrorCode {
    AMOUNT_NEGATIVE,
    AMOUNT_TOO_PRECISE,
    CONCURRENT_UPDATE,
    GRANTED_AMOUNT_NOT_POSITIVE,
    ROLE_REQUIRED,
    TRANSLATION_MISSING
}
