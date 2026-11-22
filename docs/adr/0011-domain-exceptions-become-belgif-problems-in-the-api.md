# 11. Domain exceptions become Belgif problems in the API, not in the domain

Status: accepted

## Context

Belgif's rest-problem library lets an application throw problems: exceptions that already are an HTTP response, with a problem type, a status and a link to its documentation. The library's guide shows them thrown from REST controllers and says nothing about other layers. Belgif's REST guide doesn't either.

## Decision

The domain throws its own exceptions ([ADR 10](0010-failures-are-codes-in-four-families.md)) and never a Belgif problem. The API maps them in one handler, `DomainProblemHandler`, and renders the result through the library's `ProblemMediaType`, the same call the library's own handler makes. Business problem types are built with the library's `DefaultProblem`.

## Alternatives considered

Throwing Belgif problems from the domain would save that handler, about fifty lines.

One `@ProblemType` class per business problem is the library's pattern for types a Java client should turn back into classes. This API's client is the Angular app, so those classes would add 51 files and nothing else.

## Consequences

The domain stays free of HTTP. The same use cases also run from the audit listener, where a status code means nothing, and the domain's tests need no web stack.

The library hasn't reached 1.0 and has broken compatibility in several minor releases; only the api module absorbs those upgrades.

The API alone decides how much a failure reveals, such as a single 404 for every kind of not-found.
