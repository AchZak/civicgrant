# 46. Security checks on every change

Status: accepted

## Context

A leaked credential or a vulnerable dependency should be caught when it enters the repository, not after it reaches an environment.

## Decision

Secret scanning blocks a push that contains a credential, CodeQL analyses the Java code and the workflows, and Dependabot alerts report vulnerable dependencies. Renovate stays the only tool that opens update pull requests. Vulnerabilities can be reported privately, as [SECURITY.md](../../.github/SECURITY.md) explains.

## Consequences

Dependabot only reports, so update pull requests come from one tool.
