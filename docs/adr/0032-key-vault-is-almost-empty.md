# 32. Key Vault is almost empty, on purpose

Status: accepted

## Context

The app reaches PostgreSQL and Blob Storage, and both accept a Microsoft Entra sign-in.

## Decision

The app reaches both with its managed identity, so neither has a credential. Key Vault only holds what can't be made passwordless.

## Consequences

There's no database or storage credential that could leak or would need rotating.
