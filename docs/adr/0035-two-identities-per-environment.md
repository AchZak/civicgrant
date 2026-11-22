# 35. Each environment has two identities, each allowed one job

Status: accepted

## Context

A pipeline identity that can change anything is a large target: whoever compromises the pipeline gets the same rights.

## Decision

A deploy identity, trusted only by GitHub jobs running in that environment, may deploy into the environment's resource group and nowhere else. It may grant exactly one role there, the right to send telemetry, and only to an application identity: a condition on its role administrator assignment enforces that.

An application identity runs the app and may pull images from one repository only, through a condition on its registry role.

Both live with the shared resources.

## Alternatives considered

A single "Owner" identity would have been simpler, but a compromised pipeline could then change anything in the subscription.

## Consequences

Deleting and recreating an environment keeps both identities and everything granted to them.
