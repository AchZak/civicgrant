# 44. Main only receives releases from develop

Status: accepted

## Context

I'm the only developer, and `main` should only ever hold what was released from `develop` after passing every check.

## Decision

I push directly to `develop`. `main` accepts only a pull request from `develop`, merged with a merge commit, once the build, the tests and the image build have passed. A small workflow fails any pull request into `main` that doesn't come from `develop`. Neither branch can be force-pushed or deleted.

## Alternatives considered

Squash merges would make the two branches drift apart on every release. GitHub rulesets can't say which branch a pull request must come from, which is why the workflow exists.

## Consequences

No approval is required: as the only developer, I can't approve my own pull requests.
