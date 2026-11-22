# 20. Every error is a Belgif problem

Status: accepted

## Context

Clients need one error format they can rely on and translate. Some errors happen before any controller runs: Spring Security rejects a token in its own filters.

## Decision

Errors are Problem Details (RFC 9457) as Belgif defines them, rendered by Belgif's own library. The `type` is the identifier clients rely on and translate: standard problems such as `urn:problem-type:belgif:badRequest`, with one issue per invalid field, `resourceNotFound` or `noAccessToken`, and one business problem per domain code, such as `urn:problem-type:civicgrant:submissionAfterClosingDate`.

A broken business rule answers 400, not 422, because Belgif's list of status codes has no 422. A request body that refers to something that doesn't exist, such as an unknown funding call when creating an application, is a 400 with a `referencedResourceNotFound` issue pointing at the field, not a 404: the URL exists, the reference in the body doesn't.

Spring Security's entry point hands its problem to Spring MVC, and the library renders it like every other error. Each operation in the contract lists the problems it can return, and Belgif's problem schemas are copied into the contract unchanged rather than downloaded during the build.

## Alternatives considered

Spring's default problems would have been one dependency fewer, but their `type` is `about:blank` and they don't say which field is wrong.

## Consequences

One place decides what an error looks like. A 404 looks the same whether the item is missing or hidden, and a 500 never reveals the exception. How domain failures become these problems is [ADR 11](0011-domain-exceptions-become-belgif-problems-in-the-api.md).
