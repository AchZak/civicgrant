# 48. The read side of a search has its own package

Status: accepted, partly superseded by [ADR 50](0050-the-core-separates-the-domain-from-the-application.md): the package is now `<aggregate>.application.search`, in the application layer

## Context

[ADR 47](0047-every-aggregate-package-has-the-same-layout.md) kept everything but the events in an aggregate's `model` package, including the read models its outbound ports return. For grant applications that's three types: the criteria a search takes, the summary it returns for each application, and the page that holds them. They're not part of the aggregate and carry none of its rules: they describe a list for a screen. In `model` they sat between the aggregate's value objects, and nothing showed they belong to the search.

## Decision

The types a search takes and returns live in `<aggregate>.model.search`: `ApplicationSearchCriteria`, `ApplicationSummary` and `ApplicationPage`. Visibility stays in `model`, since the aggregate applies it to itself as well.

The other rule of ADR 47 still holds: a concept gets a subpackage once it has several classes of its own and nothing package-private ties it to `model`. The search qualifies; an assessment or a decision, with two types each and the package-private `RequiredText`, doesn't.

## Consequences

`model` holds the aggregate and the values its rules are about, and the search port's imports show which types are the read side. The persistence adapter and the API mapper depend on `model.search` for lists and on `model` for single applications.
