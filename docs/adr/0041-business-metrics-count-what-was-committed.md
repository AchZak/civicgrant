# 41. Business metrics count what was committed, not what was tried

Status: accepted

## Context

Next to the technical metrics, the workflow has numbers of its own: how many applications reach each step, and how much is granted. A refused or rolled-back approval must never show up in them.

## Decision

The app counts how many applications reach each step of the workflow (created, submitted, assessed, approved, rejected) and records every granted amount. A listener counts them only after the transaction has committed. They're plain Micrometer meters.

## Consequences

The Application Insights agent already collects Spring Boot's metrics, so there's no exporter to configure. Micrometer only takes doubles, which is fine for a statistic; the budget itself stays in exact decimals in the database. The monitoring workbook queries the meters by name, so a test pins the names.
