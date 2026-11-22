# 12. The audit trail is built from domain events, and holds no free text

Status: accepted

## Context

Every relevant action must be on record. Audit rows are append-only, so personal data written there could never be erased on request, as the GDPR requires.

## Decision

Every state change records an event, and the compiler checks that each event says how it's recorded: as one audit entry, or as none when another entry already holds it.

Entries hold ids, codes and amounts, never what people typed, such as a description, a comment, a motivation or a file name.

The domain publishes its events after saving, and a listener writes the entry in the same transaction as the change. Each entry is about exactly one subject, an application, an organisation or a funding call, with a foreign key to it.

## Consequences

There's never a change without its history, nor history of a change that was rolled back: if the entry can't be written, the change is undone too.

A grant taken from a call's budget has no entry of its own: it's already in the approval's entry, with its amount. The database refuses a row with no subject or with more than one. Organisations registered on the test environment before the audit was switched on have no entry.

The audit only listens: ArchUnit fails the build if anything else in the core depends on it.
