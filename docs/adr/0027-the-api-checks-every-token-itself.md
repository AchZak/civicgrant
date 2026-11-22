# 27. The API checks every token itself, and won't start without knowing what to check

Status: accepted

## Context

Spring Boot silently skips the issuer or audience check when its setting is missing. A token Entra issues to an application, rather than to a person, must not be accepted.

## Decision

Spring Security verifies the signature against the tenant's published keys, then the issuer, the audience (the environment's client id) and the expiry. The app's own validator then requires the `access_as_user` scope, which a token issued to an application never carries, and the user's object id. The app refuses to start without an issuer and an audience.

One class turns the token into the domain's actor: the object id identifies the user, and the app roles become domain roles. Roles the domain doesn't know are ignored.

Locally, the app accepts tokens of the test registration.

## Consequences

There's no session and no user lookup: every request carries its own proof. A misconfigured environment fails at startup instead of accepting tokens it shouldn't.

The local issuer and audience are committed, because they're public identifiers that the Angular app will publish in its bundle anyway.
