# 28. One PostgreSQL server for all environments, to keep costs low

Status: accepted

## Context

This is a personal project, so the costs have to stay low, while each environment still needs its own data.

## Decision

One PostgreSQL server serves every environment. Each environment has its own database and its own Managed Identity, which can only reach that database.

The server lives with the shared resources, not in an environment, and a delete lock stops anyone removing it by mistake. Sign-in is Microsoft Entra only: password authentication is off. It's sized for the free services, with storage auto-grow off, because billing counts provisioned storage, not data.

## Consequences

Deleting and recreating an environment never touches the data. No database password exists anywhere. A real deployment would give each environment its own server.
