# 47. Every aggregate package has the same layout, and its events sit apart

Status: accepted

## Context

The domain has three aggregates and an audit trail. Someone who knows where things are in one of them should find them in the others without searching. The grant application's `model` package had grown to 28 types of six kinds: the aggregate and its snapshot, ids and value objects, the supporting document, nine event types, read models, a policy and the error codes. The audit trail and the listeners in starter only need the events, but imported them from the same package as everything else.

## Decision

Each aggregate has its own package with the same subpackages:

- `model`: the aggregate, its value objects and entities, policies, the read models its outbound ports return, and its error codes
- `model.event`: the sealed event interface and its events
- `port.in`: one use case interface per action, next to its command or query record
- `port.out`: repositories and other outbound ports
- `service`: one class per use case

Only the events get a subpackage of their own. Without Java modules, a sealed interface and the records it permits must share a package, so they move together. Everything else stays in `model`: splitting it further would force package-private helpers such as `RequiredText` to become public, and a concept only gets a subpackage once it has several classes of its own.

The shared kernel follows the same idea: `shared.event` holds what every aggregate's events build on, `DomainEvent` and the `DomainEventPublisher` port, next to `shared.error`, which holds the failure mechanism.

## Alternatives considered

- Keeping the events in `model`: fewer packages, but the largest package mixes six kinds of class, and imports don't show what other code depends on.
- One package per use case, holding its port, command and service: common in vertical slice designs, but it scatters an aggregate's ports over many packages and still needs most classes public.

## Consequences

Classes in different subpackages have to see each other, so nearly every domain class is public. The compiler doesn't stop the API from calling a service or an outbound port; ArchUnit does, and fails the build when something is used from the wrong place.

The audit trail and the listeners import their events from `model.event`, so their imports show what they react to.
