# 17. The database and the domain agree on every limit

Status: accepted

## Context

The schema repeats the most important rules as constraints, as a safety net. If the two disagreed, a valid aggregate could fail to save, or a user's mistake could surface as a database error.

## Decision

Each `CHECK` constraint and column length in the schema has a matching domain rule with the same bound. The domain also enforces what the schema can't express, such as the length of a description stored as `TEXT`. Text is measured after removing leading and trailing spaces, because that's what gets stored.

## Consequences

A valid aggregate always fits its row, and a violated constraint means a bug rather than a user mistake.
