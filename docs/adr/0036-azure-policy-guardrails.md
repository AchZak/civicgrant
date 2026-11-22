# 36. Azure refuses what breaks the project's rules, through Azure Policy

Status: accepted

## Context

The templates follow a few rules:

- Resources stay in the two chosen regions.
- Storage accepts no shared keys, is never public, and requires HTTPS with TLS 1.2 or later.
- The registry has no admin user and no anonymous pull.
- Every Container App is HTTPS-only and has a managed identity.
- Log Analytics and Application Insights only accept data sent with an Entra sign-in.

A template can only follow these rules. It can't stop a change made from the CLI or the portal.

## Decision

One Azure Policy initiative, built from Microsoft's own definitions, turns each of these rules into a Deny rule. The initiative is assigned to each CivicGrant resource group, not to the subscription, which also holds unrelated resources in other regions.

A new or changed rule is first deployed in report-only mode, and only switched on once a compliance scan shows nothing it would refuse. Each rule is pinned to the major version of its definition.

Tags work the other way round: a resource without the project's tags inherits them from its resource group. The assignment adds the tags as its own managed identity, which may change tags in its resource group and nothing else, instead of the Contributor role Microsoft's definition suggests.

## Alternatives considered

Left out on purpose: PostgreSQL's Entra-only rule, still in preview and only able to report (the server allows Entra sign-in only anyway), and refusing PostgreSQL's public endpoint, which would need the private networking the README lists as out of scope.

## Consequences

Azure refuses any request that breaks one of the rules, whether it comes from a template, the CLI or the portal, with a message pointing to these decisions.

The pipeline's deploy identities can't change policy or role assignments, so a pipeline can't loosen the rules it's held to: that takes an Owner deploying the shared templates by hand.

Microsoft's fixes to a definition apply by themselves, but a change in what a rule checks waits until I raise the pin. Inherited tags cover resources Azure creates on its own, such as Application Insights' alert action group, which a rule refusing untagged resources would one day block.

Azure Policy costs nothing for Azure resources.
