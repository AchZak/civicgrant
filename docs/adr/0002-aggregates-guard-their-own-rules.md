# 2. Aggregates guard their own rules

Status: accepted

## Context

A grant application goes through a fixed workflow, with rules on who may act, when, and with what data. Those rules must hold whichever use case changes the application. Data that was valid when it was saved must stay readable when a rule changes later.

## Decision

A grant application changes only through methods named after what happens, such as `submit`, `recordAssessment` or `approve`, never through setters. Each method checks who is acting, then the status and dates, then the data, and either succeeds completely or throws.

An aggregate has two ways in: `create` runs the creation rules and records an event, and `restore` rebuilds it from storage without running any rule. The grant application is restored from one snapshot record, which still refuses stored data that contradicts its status, such as an approved application without a decision.

Aggregates refer to each other by id. A rule that needs another aggregate receives it read-only: submitting reads the funding call's dates and cap, but only the funding call changes its own budget.

## Consequences

The rules are tested on the aggregate itself, in unit tests named after the rule, without services or a database.

Queries may return an aggregate to the API, which then maps it, and ArchUnit fails the build if anything outside the core calls more than its accessors, so a change can't bypass its use case and transaction. It also fails the build if anything but persistence calls `restore`, since that skips every rule.

Each aggregate has its own package, and ArchUnit fails the build if two of them ever depend on each other in a cycle.
