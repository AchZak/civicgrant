# 18. An assessment and a decision share their application's id

Status: accepted

## Context

An application has at most one assessment and at most one decision, and the application decides whether they may exist.

## Decision

In the database, the application's id is also the key of its assessment and of its decision, instead of a separate id with a uniqueness constraint next to it. They're saved with their application.

## Consequences

A second assessment or decision can't even be stored.
