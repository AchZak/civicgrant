# 25. No user table

Status: accepted

## Context

Everyone signs in with Microsoft Entra ID, which already knows who they are and which roles they hold.

## Decision

Users live in Entra ID, the only user store. The database only stores each user's Entra object id next to the data they own, to record who did what.

## Consequences

CivicGrant keeps no accounts and no passwords, and there's no registration step. The API knows people by their object id only, without a display name.
