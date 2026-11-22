# 45. Repository settings are code

Status: accepted

## Context

Repository protection clicked together in a settings page isn't reviewed, and can't be rebuilt.

## Decision

The rulesets are JSON files in `.github/rulesets`, and each GitHub Environment is a JSON file in `.github/environments`, next to the list of branches allowed to deploy to it. A script applies them together with the security settings and can be rerun safely: it creates what's missing, replaces what changed, and removes branches no longer listed. The test environment admits `develop` only.

## Consequences

A change to how the repository is protected is reviewed like any other change. A workflow on any other branch can't obtain the test deploy identity's token, even if it edits its own workflow file.
