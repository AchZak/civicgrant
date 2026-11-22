# 8. One search serves applicants and staff, and the visibility rule is written once

Status: accepted

## Context

Applicants list their own applications; assessors and decision makers list everything except other people's drafts. Reading a single application needs the same rule, and a page must never hold an application the caller couldn't open.

## Decision

The domain derives who may see what from the caller's roles, in one small type, `ApplicationVisibility`, that reading a single application uses too, and the database applies it in the query. Results come most recently changed first, then by id.

The query is built with the JPA Criteria API, adding only the conditions a request needs, and names columns through Hibernate's generated static metamodel (`GrantApplicationJpaEntity_.updatedAt`).

## Alternatives considered

Spring Data's `Specification` support, whose interface also offers bulk updates and deletes that would bypass the version check.

A fixed JPQL query in `@Query`, which would need "is null or" tricks for the optional filters and would give PostgreSQL one plan for every combination of them. Fixed queries elsewhere do use `@Query`.

## Consequences

Going from page to page never repeats or skips an item. The query reads only the columns a list shows. A renamed field breaks the build instead of the query.
