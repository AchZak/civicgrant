# 4. Closed sets of outcomes are sealed types

Status: accepted

## Context

Some outcomes come in a closed set where each case carries different data: an approval has a granted amount, a rejection has a formal ground.

## Decision

An assessment recommends `Approve(proposedAmount)` or `Reject()`, a decision is an `Approval` with a granted amount or a `Rejection` with a formal ground, and a grant application records one of eight events. Each set is a sealed interface of records.

## Consequences

A rejection with a granted amount can't even be built. A `switch` over one of these sets needs no `default`, so adding a ninth event breaks the build until the audit trail says how to record it. The API maps the same types with exhaustive switches.
