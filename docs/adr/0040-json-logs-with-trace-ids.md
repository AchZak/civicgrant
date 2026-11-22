# 40. Logs are JSON on Azure, and each line knows its request

Status: accepted

## Context

Log Analytics can filter fields, but plain text has to be parsed first. A warning in the logs should lead straight to the request that caused it.

## Decision

On Azure, Spring Boot writes every console line as one JSON object in the Elastic Common Schema, a format it has built in, switched on by an environment variable on the Container App. On a laptop and in tests the same app writes plain text.

The Application Insights agent adds the request's trace id to every line written during a request, the same id Application Insights gives the request itself.

## Alternatives considered

Micrometer Tracing would have added the trace id too, with one more dependency.

## Consequences

Log Analytics stores the level, the logger and the message as fields that can be filtered. Local logs stay easy to read. A warning in the logs leads straight to the request that caused it.
