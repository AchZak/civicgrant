# CivicGrant

A grant application portal for a (fictional) Belgian public administration. Non-profit organisations apply for funding through a call for projects, a civil servant assesses each application, and a separate decision maker approves or rejects it with a written motivation.

I built this to get hands-on with Azure on a project that looks like the applications I work on day to day: a real workflow, real rules, two languages, and more than one kind of user.

## How it works

An administrator opens a funding call, for example "Neighbourhood Projects 2026", with a budget of 50,000 euros, a maximum of 15,000 euros per application and a closing date.

1. An organisation creates an application in Dutch or French, attaches a PDF and submits it before the deadline.
2. A civil servant checks eligibility, writes an assessment and proposes an amount.
3. A decision maker approves or rejects. Every approval is taken from the call's budget.
4. The applicant follows the status of their application and sees the outcome with its motivation.

Applications are evaluated one by one as they come in (rolling evaluation). Good projects are funded until the budget runs out. See [Decisions](#decisions) for why it isn't a competitive ranking.

### Roles

| Role | Can do |
|---|---|
| Applicant | Create, edit and submit their own applications, upload documents, follow progress |
| Assessor | See submitted applications, record an assessment |
| Decision maker | Approve or reject assessed applications |

Staff sign in with Microsoft Entra ID and get their role through app roles.

## Business rules

- The enterprise number (KBO/BCE) must be valid, including its mod-97 checksum.
- Only certain legal forms can apply (non-profits by default).
- An application can't be submitted after the call has closed.
- The requested amount can't exceed the call's per-application cap, or the total cost of the project.
- Applicants only see their own applications.
- Status changes follow a fixed path: `DRAFT -> SUBMITTED -> ASSESSED -> APPROVED | REJECTED`.
- The person who assessed an application can't also decide on it.
- An approval can't exceed what is left of the call's budget, even when two decision makers approve at the same time.
- Every decision needs a motivation, and a rejection needs one of the formal grounds (ineligible, budget exhausted, incomplete, insufficient quality). Belgian administrative law requires public decisions to be motivated, so this isn't optional.
- Every relevant action is written to an audit trail.

The rules live in the domain model and are covered by unit tests named after the rule. The database repeats the most important ones as constraints, as a safety net.

## Architecture

Hexagonal architecture, split into four Maven modules so the dependency direction is enforced by the build and not just by convention.

```
civicgrant/
├── domain/        aggregates, business rules, use cases and ports. Plain Java.
├── api/           OpenAPI contract, generated interfaces, REST controllers
├── persistence/   JPA and PostgreSQL, Flyway migrations, Blob Storage
├── starter/       Spring Boot application, configuration and wiring
└── infra/         Bicep templates for the Azure infrastructure
```

`api` and `persistence` depend on `domain`, never on each other. `starter` puts everything together. The domain doesn't know about Spring, JPA or Azure, and ArchUnit tests check that it stays that way.

The API is contract-first: `api/src/main/openapi/openapi.yaml` is written by hand and is the single source of truth. The Spring interfaces and the Angular client are generated from it, and the pipeline lints it with Redocly on every change. The application serves the API only, not its documentation.

**Backend:** Java 25, Spring Boot 4, Spring Security (OAuth2 resource server), Spring Data JPA, Flyway, PostgreSQL
**Frontend:** Angular with standalone components, signals, zoneless change detection, Dutch and French
**Testing:** JUnit, Testcontainers, ArchUnit, Playwright

## Azure

| Service | Used for |
|---|---|
| Container Registry | The application image, shared by all environments |
| Container Apps | Hosting the backend |
| Static Web Apps | Hosting the Angular frontend |
| Database for PostgreSQL (Flexible Server) | Application data |
| Blob Storage | Uploaded PDFs, in a private container |
| Entra ID | Staff authentication and app roles |
| Managed Identity | Passwordless access to PostgreSQL and Blob Storage |
| Key Vault | The few secrets that can't be avoided |
| Application Insights | Logs, metrics and request tracing |

All infrastructure is defined in Bicep. GitHub Actions builds one image per commit and promotes it through three environments:

| Trigger | Environment |
|---|---|
| Push to `develop` | Test |
| Push to `main` | Acceptance |
| Manual approval after acceptance | Production, with the same image |

The pipeline authenticates to Azure with workload identity federation, so there are no stored credentials anywhere, including in CI.

## Decisions

**Rolling evaluation instead of a competitive call.** Many calls for projects collect everything until the deadline, score it and fund from the top down. That needs scoring criteria, a ranking step and bulk decisions. Rolling evaluation is a legitimate model, often used for smaller grants, and keeps the scope focused on the workflow.

**No message broker.** Notifications are handled in-process after the transaction commits. With one application and no other system consuming its events, a broker would add cost and moving parts without solving anything. I'd introduce Azure Service Bus with a transactional outbox the moment another system needs these events, for example a finance system that pays out approved grants.

**Key Vault is almost empty, on purpose.** Managed Identity removes the database and storage credentials. Key Vault only holds what can't be made passwordless.

**No user table.** Users live in Entra ID. The database only stores their object id to record who did what.

**Budget protected by optimistic locking.** The call keeps the amount already committed, and its version column makes two concurrent approvals conflict instead of both succeeding.

**Ids are generated by the domain.** An aggregate has its identity before it's saved.

**One image for every environment.** The image contains the application only. Everything that differs between environments, such as the database, the identity and the monitoring target, is set as environment variables on each Container App. The image that passed acceptance is exactly the one that goes to production.

**One PostgreSQL server for all environments, to keep costs low.** Each environment has its own database and its own Managed Identity, which can only reach that database. A real deployment would give each environment its own server.

**Shared resources are bootstrapped by hand, once.** The registry and the pipeline's identity are deployed from a developer machine, because the pipeline can't create the identity it logs in with. Everything else is deployed by the pipeline.

**A schema, not tablespaces.** Azure Database for PostgreSQL doesn't support custom tablespaces, and physical storage isn't a migration's concern anyway. Everything lives in the `civicgrant` schema.

## Running it locally

Requirements: JDK 25 and Docker.

```bash
./mvnw clean install
./mvnw -pl starter spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile starts PostgreSQL through `compose.yaml`, keeps its data between runs and loads demo data from `db/seed`. Tests don't use it: they run against a throwaway PostgreSQL started by Testcontainers.

## Status

- [x] Database schema
- [x] Multi-module structure
- [ ] Domain model and rules
- [ ] OpenAPI contract
- [ ] Applicant flow
- [ ] Assessment and decision flow
- [ ] Angular frontend
- [x] CI pipeline: formatting, tests and SonarQube analysis
- [x] Container image
- [ ] Shared Azure infrastructure: registry and pipeline identity
- [ ] Test environment
- [ ] Acceptance and production

## Out of scope

Things a real system would need, left out to keep the project finishable:

- Citizen and organisation login through the Federal Authentication Service (eID, itsme), and verification that the applicant may represent the organisation.
- Private networking (VNet integration, private endpoints) and API Management.
- Payments of approved grants.
- Appeals against a decision.
- Email delivery. Notifications are shown in the application.
