# 7. Nobody decides alone, or on their own application

Status: accepted

## Context

A public grant needs a second pair of eyes: the person who assessed an application shouldn't also approve it, and nobody should judge their own application.

## Decision

Whoever assessed an application can't decide on it (`SEPARATION_OF_DUTIES`), and an applicant can't assess or decide on their own (`CONFLICT_OF_INTEREST`), even with every role.

Both are domain rules, not database constraints: they compare people stored in different tables, which a `CHECK` constraint can't do.

## Consequences

The test environment has two staff accounts, members of the tenant's own domain at no cost, so the rules are also checked there with real tokens: one assesses, the other decides, and the assessor's attempt to decide answers 403.
