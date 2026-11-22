# 19. The API follows the Belgif REST guidelines

Status: accepted

## Context

Belgif, the Belgian public sector's interoperability framework, publishes a guide for REST APIs, which Belgian administrations follow for their own APIs.

## Decision

The contract, `api/src/main/openapi/openapi.yaml`, is written by hand and follows the guide:

- Paths are in lowerCamelCase (`/fundingCalls`).
- Collections are objects with an `items` list, and pagination uses `page`, `pageSize`, `total` and links to the page itself and to the previous and next ones.
- A field without a value, such as the submission time of a draft, is left out of the JSON rather than sent as `null`, as the contract describes it.
- Lifecycle steps are resources named by a noun, never a verb in the path: an applicant creates `/applications/{id}/submission` with POST, and anyone allowed can read it with GET. The same goes for the assessment and the decision, each with its own access rule.
- A decision is sent as one flat body: an outcome with either a granted amount or a rejection ground.
- Each resource has its own tag.

## Alternatives considered

A choice between two schemas (`oneOf`) for the decision, which the Java code generator handles poorly.

## Consequences

The assessment stays internal while the applicant reads the decision and its motivation. When a decision's outcome and its field don't match, the answer is a 400 naming the field. Each generated interface is implemented by exactly one controller.

The contract is the single source of truth: the Spring interfaces and the Angular client are generated from it, and the pipeline lints it with Redocly on every change.
