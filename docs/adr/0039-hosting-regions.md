# 39. Hosted in Belgium Central, with the database in France Central

Status: accepted

## Context

A Belgian administration's data should stay at rest in Belgium. When I created the database, Belgium Central had no capacity left for its size, twice. I tried again before adding Blob Storage, and Belgium Central still had no capacity.

## Decision

I chose Belgium Central, at the same price as West Europe. The database runs in France Central, with each environment's application next to it, and the documents' storage account is in France Central too.

## Consequences

Keeping the application next to the database avoids queries that cross regions, which would add latency and cost. The data stays in the EU, but not in Belgium. A real administration would reserve capacity in Belgium Central instead.

Belgium Central has no paired region, which only matters for geo-redundant backups, and this project doesn't use them.
