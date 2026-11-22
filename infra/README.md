# Infrastructure

Every Azure resource CivicGrant uses is defined here in Bicep, and so are the API's app registrations in Microsoft Entra ID. Nothing exists only in the portal.

The templates are split by who deploys them, because each deployer holds different rights: an Owner of the subscription, a tenant administrator, or the pipeline.

## Layout

```
infra/
├── shared.bicep                  what every environment shares, deployed by hand
├── shared.bicepparam
├── entra.bicep                   the API's app registrations, deployed by hand
├── entra.bicepparam
├── environment.bicep             one environment, deployed by the pipeline
├── environment.tst.bicepparam    its values for the test environment
├── modules/
│   ├── shared/                   used by shared.bicep
│   ├── entra/                    used by entra.bicep
│   └── environment/              used by environment.bicep
├── bootstrap/                    what Bicep can't do, as runbooks
├── workbooks/                    the monitoring workbook, loaded by environment.bicep
└── bicepconfig.json              linter rules and the Microsoft Graph extension
```

A parameter file starts with the name of its template, so acc and prd will sit next to `environment.tst.bicepparam`. A module's folder is named after the entry point that uses it, so it also says who deploys it. That's why `modules/shared/` holds `environment-identities.bicep`: an environment's identities are created by the shared deployment, so the pipeline never creates the identity it logs in with.

## Entry points

| Template | Deployed into | By | Creates |
|---|---|---|---|
| `shared.bicep` | the subscription | an Owner, by hand | `rg-civicgrant-shared`, holding the container registry, the pipeline identity, the PostgreSQL server with one database per environment, the documents storage account with one container per environment, and the alert action group. For each environment: its empty resource group, a deploy identity and an app identity, and their roles. Also the Azure Policy guardrails and the monthly budget |
| `entra.bicep` | `rg-civicgrant-shared`, which only keeps the deployment record | a tenant administrator, by hand | one app registration per environment, `civicgrant-api-<env>`, with its scope and app roles |
| `environment.bicep` | `rg-civicgrant-<env>` | the pipeline, as the Deployment Stack `stack-civicgrant-<env>` | Log Analytics, Application Insights, the Container Apps environment, the Container App, the alert rule and the monitoring workbook |

The pipeline's deploy identity has rights on its own environment's resource group only. Everything outside it (the shared resources, the identities, the policies, Entra ID) is deployed by hand by someone who holds those rights. [ADR 34](../docs/adr/0034-shared-resources-are-bootstrapped-by-hand.md), [ADR 35](../docs/adr/0035-two-identities-per-environment.md) and [ADR 38](../docs/adr/0038-deploy-to-test-through-a-deployment-stack.md) explain the split.

## Setting up from nothing

1. Deploy `shared.bicep`, with `BUDGET_ALERT_EMAIL` exported in your shell: the address the budget and the alerts write to, kept out of the repository.
2. Deploy `entra.bicep`.
3. Follow the runbooks in `bootstrap/`: [database-role.md](bootstrap/database-role.md) creates each environment's database role, [app-roles.md](bootstrap/app-roles.md) gives users their roles on the API.
4. Copy the deployments' outputs into GitHub variables: shared values at repository level, each environment's identities and API client id on its GitHub Environment. None of them is a secret.
5. Push to `develop`. The pipeline previews the stack's changes, deploys `environment.bicep` with `environment.tst.bicepparam`, and checks that the app is healthy.

The Bicep deployments can run again at any time: they update what exists instead of duplicating it.

Each parameter file gives every value its entry point takes. Values that come out of another deployment, such as the registry's login server or the app identity's client id, are read with `readEnvironmentVariable`: from GitHub variables in the pipeline, from your shell on your machine.

## Checking a change

```bash
az bicep lint --file infra/environment.bicep
az deployment sub what-if --location belgiumcentral --name civicgrant-shared --parameters infra/shared.bicepparam
az deployment group what-if --resource-group rg-civicgrant-tst --parameters infra/environment.tst.bicepparam
```

Lint each entry point the same way. The tst what-if needs the variables `environment.tst.bicepparam` reads, exported in your shell. `entra.bicep` has no what-if, because Azure can't preview Microsoft Graph resources: review the diff, deploy, then check the result with `az ad app show`.

## Conventions

- Names follow the Cloud Adoption Framework abbreviations: `rg-civicgrant-tst`, `ca-civicgrant-tst`. Names that must be globally unique get a `uniqueString` suffix.
- Every resource carries the tags `project`, `environment` and `managedBy`. The guardrails copy them from the resource group when one is missing.
- No keys, passwords or connection strings. The app signs in to PostgreSQL, Blob Storage and Application Insights with its managed identity, and the pipeline signs in to Azure with GitHub's OIDC token. Azure Policy refuses a resource that turns shared keys, admin users or anonymous access back on ([ADR 36](../docs/adr/0036-azure-policy-guardrails.md)).
- Shared resources live in Belgium Central; PostgreSQL, the documents storage and the environments in France Central ([ADR 39](../docs/adr/0039-hosting-regions.md)).
- Costs stay at a few euros a month: one PostgreSQL server for every environment ([ADR 28](../docs/adr/0028-one-postgresql-server-for-all-environments.md)), Container Apps that scale to zero, and a €5 budget that alerts from the first euro.
