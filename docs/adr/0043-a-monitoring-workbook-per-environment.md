# 43. Each environment has its own monitoring workbook

Status: accepted

## Context

A dashboard changed by hand in the portal isn't reviewed, and drifts from one environment to the next.

## Decision

Each environment has an Azure Monitor workbook, "CivicGrant" plus the environment's name. It shows requests and errors per operation, the grant workflow with the amounts granted, and warning and error log lines next to their request. Its queries live in a JSON file in the repository and are deployed with Bicep. Health probes are left out of every query.

## Consequences

A change to the dashboard is reviewed like code, and the next deployment overwrites anything changed by hand in the portal. Health probes are almost all of the traffic, so leaving them out keeps the numbers about real use. A workbook has no price of its own, and queries on the tables it reads aren't charged.
