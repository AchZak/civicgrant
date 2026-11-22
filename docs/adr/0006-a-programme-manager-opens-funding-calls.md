# 6. Funding calls are opened by a role of their own

Status: accepted

## Context

Opening a call sets the dates and the budget every grant is taken from. That's a programme's job, not an assessor's or a decision maker's.

## Decision

A fourth role, `PROGRAMME_MANAGER`, which can be given on its own, opens funding calls. The domain checks the call's rules, and the database keeps its reference unique.

## Consequences

When two managers pick the same reference, the second gets a 409. The opening goes into the audit trail with the reference and the total budget, so it's on record who set a budget. The role may read funding calls and nothing else, as [ADR 5](0005-roles-are-checked-by-the-domain.md) describes.
