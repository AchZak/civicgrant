# 31. A schema, not tablespaces

Status: accepted

## Context

Azure Database for PostgreSQL doesn't support custom tablespaces, and physical storage isn't a migration's concern anyway.

## Decision

Everything lives in the `civicgrant` schema, created by Flyway.

## Consequences

Migrations describe tables, not where they're stored. Every pooled connection sets its search path to `civicgrant`, so native SQL in the adapters names no schema.
