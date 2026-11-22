package be.achzak.civicgrant.domain.organisation.model;

import be.achzak.civicgrant.domain.shared.error.ErrorCode;

public enum OrganisationErrorCode implements ErrorCode {
    ENTERPRISE_NUMBER_INVALID_CHECKSUM,
    ENTERPRISE_NUMBER_INVALID_FORMAT,
    ORGANISATION_ALREADY_REGISTERED,
    ORGANISATION_NAME_REQUIRED,
    ORGANISATION_NAME_TOO_LONG,
    ORGANISATION_NOT_FOUND
}
