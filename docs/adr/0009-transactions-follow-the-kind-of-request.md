# 9. Transactions follow the kind of request, not annotations

Status: accepted

## Context

The domain has no Spring annotations, so a use case can't declare `@Transactional` itself.

## Decision

Every use case takes exactly one command or one query: a record that implements the domain's plain `Command` or `Query` interface. The application wraps it accordingly: a command runs in a read-write transaction that rolls back when the domain refuses it, a query in a read-only one. The type decides, not the name.

## Consequences

ArchUnit fails the build if a use case takes anything else. A test registers an organisation through the real application against PostgreSQL, so wiring that lost the transaction fails the build rather than a request in production.
