# 50. The core separates the domain from the application

Status: accepted. Supersedes [ADR 47](0047-every-aggregate-package-has-the-same-layout.md), and [ADR 48](0048-the-read-side-of-a-search-has-its-own-package.md) for where the search's types live

## Context

The domain module held everything between the adapters: the aggregates and their rules, but also the use cases, the services that run them and the ports the adapters implement. Each aggregate package split these by kind (`model`, `port.in`, `port.out`, `service`, ADR 47), but nothing told the domain model apart from the application around it. A rule could have imported a repository port or a command and the build wouldn't have noticed, and the module's name suggested that use cases, transactions and pages of search results were domain concepts.

Hexagonal architecture has two rings inside the hexagon: the domain model, which knows only itself, and the application, which offers the use cases (inbound ports), says what it needs from outside (outbound ports) and runs the model in services.

## Decision

The module is called `core` (`civicgrant-core`) and its root package is `be.achzak.civicgrant.core`, the same pattern as `api`, `persistence` and `starter`. Under it there is one package per aggregate, plus the shared kernel and the audit trail, each with the same two layers:

- `<aggregate>.domain`: the aggregate, its value objects and entities, policies and error codes (was `model`)
- `<aggregate>.domain.event`: the sealed event interface and its events (was `model.event`)
- `<aggregate>.application.port.in`: one use case interface per action, next to its command or query record and the read models it returns
- `<aggregate>.application.port.out`: repositories and the other outbound ports
- `<aggregate>.application.service`: one class per use case
- `<aggregate>.application.search`: what a search takes and returns, where there is one (was `model.search`)

The shared kernel has the same shape: `sharedkernel.domain` (`Money`, `LocalizedText`, `Actor`, `Role`, `UserId`, `SharedErrorCode`), `sharedkernel.domain.event` (`DomainEvent`), `sharedkernel.domain.error` (the failure mechanism), `sharedkernel.application` (the `Command` and `Query` markers) and `sharedkernel.application.port.out` (`DomainEventPublisher`).

Ports in both directions belong to the application. A repository is something a use case needs, not something an aggregate uses, so the domain declares nothing an adapter implements.

The search's types move to the application because a page, an order and a summary row are about showing a list, not about the rules of a grant. The criteria still carry the domain's `ApplicationVisibility`, which the aggregate applies to itself too.

The aggregate packages are modules inside one bounded context, grant funding, not bounded contexts of their own: they share one language and one database, a grant application reads its funding call directly, and an approval changes both in one transaction (ADR 14).

ArchUnit holds the layers: the domain never depends on the application, every class in the core belongs to one of the two, and outbound ports are interfaces. The earlier rules (use case shape, services, adapters reaching the core only through the right ports, no cycles between aggregate packages, nothing depending on the audit trail) now name the new packages.

## Alternatives considered

- Repository interfaces in the domain, as Evans and Vernon place them, and only use cases in the application: defensible, but outbound ports would then live in two places (repositories in the domain, `DocumentStorage` and the event publisher in the application), and the domain would declare what persistence must implement.
- A Maven module per layer: the compiler would enforce the direction, but every aggregate would be spread over two modules, and ArchUnit already fails the build on a wrong dependency.
- The aggregate packages directly under `be.achzak.civicgrant`: shorter imports, but they'd sit next to the adapter modules in the root package, and an import would no longer show which module a class comes from.
- A shared kernel split into `model` and `identity`: it groups who acts, but gives the shared kernel a different shape from every aggregate, and `SharedErrorCode`, thrown by `Money`, `LocalizedText` and `Actor`, could only sit next to one of the two groups.

## Consequences

ADR 47's reasoning still holds one level down: inside `domain` only the events get a subpackage, and `RequiredText` stays package-private next to the rules that use it.

An import shows which layer a class belongs to. The API uses inbound ports, the read models they return and domain value types; persistence uses outbound ports, the search's types and the aggregates it restores.

Starter's `UseCaseConfiguration` (`DomainConfiguration` until this change) registers every class in `core.*.application.service`.

Earlier records say "the domain" for what was the domain module. Where they mean the module, read "the core": the role checks of ADR 5, for instance, sit in the use cases, which are now in the application layer.
