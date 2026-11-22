# 16. Reference numbers come from one PostgreSQL sequence

Status: accepted

## Context

An application needs a reference people can read and quote, such as `CG-2026-000042`, from the moment its draft is created. It must be unique even when two applicants create a draft at the same moment.

## Decision

The number comes from a database sequence, which hands out each value once, and the year from the application's clock, in UTC. The counter doesn't restart in January.

## Consequences

References stay unique without a table per year; six digits leave room for a million applications. A creation that's refused or rolled back still used its number, so there are gaps: the number is a label, not a count.
