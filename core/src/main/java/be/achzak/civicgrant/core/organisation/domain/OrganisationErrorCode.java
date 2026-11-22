package be.achzak.civicgrant.core.organisation.domain;

import be.achzak.civicgrant.core.sharedkernel.domain.error.ErrorCode;

public enum OrganisationErrorCode implements ErrorCode {
    ENTERPRISE_NUMBER_INVALID_CHECKSUM,
    ENTERPRISE_NUMBER_INVALID_FORMAT,
    ORGANISATION_ALREADY_REGISTERED,
    ORGANISATION_NAME_REQUIRED,
    ORGANISATION_NAME_TOO_LONG,
    ORGANISATION_NOT_FOUND
}
