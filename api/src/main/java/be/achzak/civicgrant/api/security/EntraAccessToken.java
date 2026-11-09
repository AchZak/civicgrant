package be.achzak.civicgrant.api.security;

final class EntraAccessToken {

    static final String OBJECT_ID_CLAIM = "oid";
    static final String SCOPES_CLAIM = "scp";
    static final String APP_ROLES_CLAIM = "roles";
    static final String API_SCOPE = "access_as_user";
    static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    private EntraAccessToken() {}
}
