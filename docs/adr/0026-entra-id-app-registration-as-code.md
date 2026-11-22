# 26. Staff sign in with Microsoft Entra ID, and the API's registration is code

Status: accepted

## Context

The API needs an app registration in Entra ID for each environment. Creating applications needs tenant-wide rights that no pipeline identity should hold.

## Decision

Each environment has its own app registration, `civicgrant-api-<env>`, declared in `infra/entra.bicep` with the Microsoft Graph Bicep extension and deployed by hand by a tenant administrator.

The API exposes one scope, `access_as_user`, which only an administrator can let a client use, and four app roles whose values are the names of the domain's roles. Its service principal requires an assignment.

Who holds which role isn't in Bicep: it changes more often than the API, and it would put user ids in a public repository. An administrator assigns users directly, following `infra/bootstrap/app-roles.md`.

## Alternatives considered

Terraform's `azuread` provider would have meant a second infrastructure tool for one file. Assigning groups instead of users would need a paid Entra ID P1 licence.

## Consequences

A token for test is refused by production, and a role in test grants nothing in production. Entra issues no token at all to a user without a role.

Azure can't preview Microsoft Graph resources or put them in a Deployment Stack, so a change is reviewed as a Git diff and checked afterwards.
