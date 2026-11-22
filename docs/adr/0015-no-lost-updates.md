# 15. Two people can't overwrite each other's change

Status: accepted

## Context

Two people can edit the same application at the same time. Without a check, the second save would silently replace the first.

## Decision

Every application has a version. Saving loads the stored row and refuses an application that was read before someone else saved it, and the database checks the version once more in the `UPDATE`. The stored row is updated in place, never replaced by a new copy, because that's what lets JPA compare the versions.

## Consequences

The second of two simultaneous edits answers 409 `concurrentUpdate` instead of silently replacing the first. The client reads the application again and retries.
