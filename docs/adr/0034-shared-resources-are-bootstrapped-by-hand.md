# 34. Shared resources are bootstrapped by hand, once

Status: accepted

## Context

The pipeline can't create the identities it logs in with.

## Decision

The registry, the pipeline's identity, the PostgreSQL server and each environment's identities and empty resource group are deployed from a developer machine. Everything inside an environment is deployed by the pipeline.

## Consequences

Changing a shared resource takes an Owner deploying the shared templates by hand, which is also what keeps a pipeline from loosening its own rights ([ADR 36](0036-azure-policy-guardrails.md)).
