# 5. Roles are checked by the domain, and the build fails if a use case forgets

Status: accepted

## Context

Role rules can live on each endpoint, in the domain, or in both. Two places drift apart, and one place should decide who may do what: the place the tests already cover.

## Decision

Every use case starts by checking the caller's role, before it loads anything, so a caller without the role learns nothing about what exists. The endpoints have no role rules of their own.

A use case lists every role it accepts, never "any role", so a role added later gets nothing until someone decides it should.

## Alternatives considered

A test calling every use case with the wrong role. It only catches the use cases someone remembered to add to its list.

## Consequences

An ArchUnit rule fails the build if a use case doesn't make that check itself, and each use case's tests prove it checks the right role.

The programme manager, added after the other roles, reads funding calls but gets a 403 on organisations and applications.

A malformed request answers 400 even to a caller without the role, because the API validates the request before the use case runs.
