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

Everyone signs in with Microsoft Entra ID, which is the only user store. CivicGrant keeps no accounts and no passwords, only each user's Entra object id next to the data they own, so there's no registration step. Staff get an account in the administration's tenant and a role from an administrator, which is how workforce applications usually work, and a user without a role gets no token at all. Applicants are a deliberate simplification: here they're users of the same tenant, given the Applicant role by an administrator. A real portal would let citizens sign in on their own, through the Federal Authentication Service (eID, itsme) or a Microsoft Entra External ID tenant with self-service sign-up, where every signed-in citizen is an applicant and only staff need a role.

## Business rules

- The enterprise number (KBO/BCE) must be valid, including its mod-97 checksum.
- Only certain legal forms can apply (non-profits by default).
- An application can't be submitted after the call has closed.
- The requested amount can't exceed the call's per-application cap, or the total cost of the project.
- An application needs at least one supporting document, a PDF of at most 10 MB, before it can be submitted.
- Applicants only see their own applications.
- Status changes follow a fixed path: `DRAFT -> SUBMITTED -> ASSESSED -> APPROVED | REJECTED`.
- The person who assessed an application can't also decide on it, and applicants can't assess or decide on their own application.
- The proposed and granted amounts can't exceed the requested amount, and an application assessed as ineligible can only be rejected.
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

**Aggregates guard their own rules.** A grant application changes only through methods named after what happens, such as `submit`, `recordAssessment` or `approve`, never through setters. Each method checks who is acting, then the status and dates, then the data, and either succeeds completely or throws. An aggregate has two ways in: `create` runs the creation rules and records an event, and `restore` rebuilds it from storage without running any rule, because data that was valid when it was saved must stay readable when a rule changes later. The grant application is restored from one snapshot record, which still refuses stored data that contradicts its status, such as an approved application without a decision. Aggregates refer to each other by id. A rule that needs another aggregate receives it read-only: submitting reads the funding call's dates and cap, but only the funding call changes its own budget. Each aggregate has its own package, and ArchUnit fails the build if two of them ever depend on each other in a cycle.

**Roles are checked by the domain, and the build fails if a use case forgets.** Every use case starts by checking the caller's role, before it loads anything, so a caller without the role learns nothing about what exists. The endpoints have no role rules of their own: one place decides who may do what, and it's the place the tests already cover. A use case open to every role still lists the three roles, so a role added later gets nothing until someone decides it should. An ArchUnit rule fails the build if a use case doesn't make that check itself, and each use case's tests prove it checks the right role. A test calling every use case with the wrong role was the alternative, but it only catches the use cases someone remembered to add to its list.

**Transactions follow the kind of request, not annotations.** The domain has no Spring annotations. Every use case takes exactly one command or one query, and the application wraps it accordingly: a command runs in a read-write transaction that rolls back when the domain refuses it, a query in a read-only one. A test registers an organisation through the real application against PostgreSQL, so wiring that lost the transaction fails the build rather than a request in production.

**Closed sets of outcomes are sealed types.** An assessment recommends `Approve(proposedAmount)` or `Reject()`, a decision is an `Approval` with a granted amount or a `Rejection` with a formal ground, and a grant application records one of eight events. Each set is a sealed interface of records, so a rejection with a granted amount can't even be built, and a `switch` over one needs no `default`: adding a ninth event breaks the build until the audit trail says how to record it.

**Failures are codes, in four families.** Every expected failure is a domain exception with a stable code, such as `SUBMISSION_AFTER_CLOSING_DATE`, which the frontend translates into Dutch or French. The English message is for logs only. The family says what kind of failure it is, and the API maps each to one HTTP status: not found (404), forbidden (403), business rule violated (400), and not allowed in the current status (409). When someone may not even know an application exists, such as an applicant asking for someone else's, or staff asking for a draft, the answer is 404 rather than 403, so ids can't be probed. A bug, such as corrupt stored data, is a plain Java exception and becomes a 500.

**Nobody decides alone, or on their own application.** Whoever assessed an application can't decide on it (`SEPARATION_OF_DUTIES`), and an applicant can't assess or decide on their own (`CONFLICT_OF_INTEREST`), even with every role. Both are domain rules, not database constraints: they compare people stored in different tables, which a `CHECK` constraint can't do.

**The audit trail is built from domain events, and holds no free text.** Every state change records an event, and each event maps to exactly one audit entry, a mapping the compiler checks for completeness. Entries hold ids, codes and amounts, never what people typed, such as a description, a comment, a motivation or a file name: audit rows are append-only, so personal data written there could never be erased on request, as the GDPR requires. Entries are meant to be written in the same transaction as the change they record, so there's never a change without its history, nor history of a change that was rolled back. The mapping and the service that stores entries exist; the listener that calls it inside the transaction comes with the grant application use cases, so organisations registered before then have no entry. The audit only listens: ArchUnit fails the build if anything else in the domain depends on it.

**The database and the domain agree on every limit.** Each `CHECK` constraint and column length in the schema has a matching domain rule with the same bound, so a valid aggregate always fits its row, and a violated constraint means a bug rather than a user mistake. The domain also enforces what the schema can't express, such as the length of a description stored as `TEXT`. Text is measured after removing leading and trailing spaces, because that's what gets stored.

**The API follows the Belgif REST guidelines.** Belgif, the Belgian public sector's interoperability framework, publishes a guide for REST APIs, and the contract follows it: paths in lowerCamelCase (`/fundingCalls`), collections as objects with an `items` list, and pagination with `page`, `pageSize`, `total` and a link to the next page. Lifecycle steps are resources named by a noun, never a verb in the path: an applicant creates `/applications/{id}/submission` with POST, and anyone allowed can read it with GET. The same goes for the assessment and the decision, and each gets its own access rule, so the assessment stays internal while the applicant reads the decision and its motivation. Each resource has its own tag, so each generated interface is implemented by exactly one controller.

**Every error is a Belgif problem.** Errors are Problem Details (RFC 9457) as Belgif defines them, rendered by Belgif's own library. The `type` is the identifier clients rely on and translate: standard problems such as `urn:problem-type:belgif:badRequest`, with one issue per invalid field, `resourceNotFound` or `noAccessToken`, and one business problem per domain code, such as `urn:problem-type:civicgrant:submissionAfterClosingDate`. A broken business rule answers 400, not 422, because Belgif's list of status codes has no 422. A 404 looks the same whether the item is missing or hidden, and a 500 never reveals the exception. Spring Security rejects a token before any controller runs, so its entry point hands the problem to Spring MVC, and the library renders it like every other error: one place decides what an error looks like. Each operation in the contract lists the problems it can return, and Belgif's problem schemas are copied into the contract unchanged rather than downloaded during the build. Spring's default problems would have been one dependency fewer, but their `type` is `about:blank` and they don't say which field is wrong.

**A created resource is read back, and its location is relative.** After a create, the API reads the new resource back and returns it, so the client sees what was stored, such as the trimmed name and the enterprise number without dots, not an echo of its own request. The `Location` header is a path, `/api/v1/organisations/{id}`, as in Belgif's own examples. An absolute URL would have to trust the proxy's forwarded headers to know it's served over HTTPS, and Spring Boot does that only on platforms it recognises, which don't include Azure Container Apps.

**One image for every environment.** The image contains the application only. Everything that differs between environments, such as the database, the identity and the monitoring target, is set as environment variables on each Container App. The image that passed acceptance is exactly the one that goes to production.

**One PostgreSQL server for all environments, to keep costs low.** Each environment has its own database and its own Managed Identity, which can only reach that database. A real deployment would give each environment its own server. The server lives with the shared resources, not in an environment, so deleting and recreating an environment never touches the data, and a delete lock stops anyone removing the server by mistake. Sign-in is Microsoft Entra only: password authentication is off, so no database password exists anywhere. It's sized for the free services, with storage auto-grow off, because billing counts provisioned storage, not data.

**Database roles are bootstrapped by hand, and each database admits only its own app.** Bicep can't create PostgreSQL roles, and no database administrator credential exists in CI. So the server's Entra administrator runs `infra/bootstrap/database-role.sql` once per environment, from Azure Cloud Shell: the firewall already admits Azure, while a rule for a home IP address would stay open for good, since the delete lock blocks removing it. The script maps a role to the environment's app identity, then takes away PostgreSQL's default "every role may connect" from the environment's database and lets in that role alone. The role may create schemas there, so Flyway creates the `civicgrant` schema and the app owns everything in it, with no further grants. The script can be rerun safely and runs as one transaction that reads back its own result before committing, because PostgreSQL only warns when a grant doesn't apply.

**The app signs in to PostgreSQL with a token, through Microsoft's JDBC plugin.** On Azure, the datasource URL names Microsoft's authentication plugin. For each new connection, the plugin asks the app's managed identity, chosen by its client id, for a Microsoft Entra token and hands it to the PostgreSQL driver as the password. The same URL makes the driver check the server's certificate and host name (`verify-full`) against the Java runtime's own trust store, which already contains Azure's root certificates, so the image carries no certificate file. Locally and in tests the URL doesn't name the plugin, so Docker Compose and Testcontainers keep signing in with a password. Spring Cloud Azure would have set this up from a single property, but its current releases support Spring Boot 4.0, not the 4.1 this project runs. The plugin is what Spring Cloud Azure uses underneath, so switching later changes configuration, not behaviour. The Azure SDK's default HTTP transport, Netty, is replaced by the JDK's built-in HTTP client: one networking stack fewer in the image, and fewer security updates to follow.

**Shared resources are bootstrapped by hand, once.** The registry, the pipeline's identity, the PostgreSQL server and each environment's identities and empty resource group are deployed from a developer machine, because the pipeline can't create the identities it logs in with. Everything inside an environment is deployed by the pipeline.

**Each environment has two identities, each allowed one job.** A deploy identity, trusted only by GitHub jobs running in that environment, may deploy into the environment's resource group and nowhere else. It may grant exactly one role there, the right to send telemetry, and only to an application identity: a condition on its role administrator assignment enforces that. An application identity runs the app and may pull images from one repository only, through a condition on its registry role. Both live with the shared resources, so deleting and recreating an environment keeps them and everything granted to them. A single "Owner" identity would have been simpler, but a compromised pipeline could then change anything in the subscription.

**The test environment costs nothing while nobody uses it, and holds no key.** The app runs on Azure Container Apps, in an environment with only the serverless Consumption profile, which has no fixed fee. It scales between zero and one replica on HTTP traffic: with no visitors nothing runs and nothing is billed, and the first visit waits for a cold start of under a minute. Half a vCPU and 1 GiB keep even a busy month far inside the free monthly allowance. Logs and telemetry go to one Log Analytics workspace, capped well below the free monthly volume. Neither the workspace nor Application Insights accepts keys: the app sends telemetry with its own managed identity, and the platform's logs are routed by a diagnostic setting. The Application Insights connection string grants nothing on its own.

**Every push to develop deploys itself, through a Deployment Stack.** The "Deploy to test" job logs in as the environment's deploy identity, writes a preview of the changes to the run's summary, and applies the templates as a Deployment Stack. The stack deletes any resource removed from the templates, so the environment always matches Git, and a deny assignment stops anyone, Owner included, from deleting its resources by hand. The job only goes green when the app reports healthy, which includes its token sign-in to the database, and when the new revision is the one serving: a revision that never becomes ready leaves the previous one running, so a health check alone could pass against the old image. Values shared by every environment are repository variables. The deploy identity and the app identity's ids are variables of the GitHub Environment, which take precedence in the jobs that use it.

**Staff sign in with Microsoft Entra ID, and the API's registration is code.** Each environment has its own app registration, `civicgrant-api-<env>`, declared in `infra/entra.bicep` with the Microsoft Graph Bicep extension. A tenant administrator deploys it by hand, because creating applications needs tenant-wide rights that no pipeline identity should hold. With one registration per environment, a token for test is refused by production, and a role in test grants nothing in production. The API exposes one scope, `access_as_user`, which only an administrator can let a client use, and three app roles whose values are the names of the domain's roles. Its service principal requires an assignment, so Entra issues no token at all to a user without a role. Azure can't preview Microsoft Graph resources or put them in a Deployment Stack, so a change is reviewed as a Git diff and checked afterwards. Who holds which role isn't in Bicep: it changes more often than the API, and it would put user ids in a public repository. An administrator assigns users directly, following `infra/bootstrap/app-roles.md`. Assigning groups would need a paid Entra ID P1 licence. Terraform's `azuread` provider would have meant a second infrastructure tool for one file.

**The API checks every token itself, and won't start without knowing what to check.** Spring Security verifies the signature against the tenant's published keys, then the issuer, the audience (the environment's client id) and the expiry. The app's own validator then requires the `access_as_user` scope, which a token issued to an application rather than a person never carries, and the user's object id. Spring Boot silently skips the issuer or audience check when its setting is missing, so the app refuses to start instead. One class turns the token into the domain's actor: the object id identifies the user, and the app roles become domain roles. Roles the domain doesn't know are ignored. There's no session and no user lookup: every request carries its own proof. Locally, the app accepts tokens of the test registration. Its issuer and audience are committed, because they're public identifiers that the Angular app will publish in its bundle anyway.

**Main only receives releases from develop.** I push directly to `develop`. `main` accepts only a pull request from `develop`, merged with a merge commit, once the build, the tests and the image build have passed. Squash merges would make the two branches drift apart on every release. GitHub rulesets can't say which branch a pull request must come from, so a small workflow fails any pull request into `main` that doesn't come from `develop`. No approval is required: as the only developer, I can't approve my own pull requests. Neither branch can be force-pushed or deleted.

**Repository settings are code.** The rulesets are JSON files in `.github/rulesets`, and each GitHub Environment is a JSON file in `.github/environments` with the list of branches allowed to deploy to it next to it. A script applies them together with the security settings and can be rerun safely: it creates what's missing, replaces what changed, and removes branches no longer listed. A change to how the repository is protected is reviewed like any other change, instead of being clicked in a settings page. The test environment admits `develop` only, so a workflow on any other branch can't obtain the test deploy identity's token, even if it edits its own workflow file.

**Security checks on every change.** Secret scanning blocks a push that contains a credential, CodeQL analyses the Java code and the workflows, and Dependabot alerts report vulnerable dependencies. Renovate stays the only tool that opens update pull requests, so Dependabot only reports. Vulnerabilities can be reported privately, as `SECURITY.md` explains.

**Hosted in Belgium Central, with the database in France Central.** I chose Belgium Central so a Belgian administration's data would stay at rest in Belgium, at the same price as West Europe. When I created the database, Belgium Central had no capacity left for its size, twice. So the database runs in France Central, with each environment's application next to it, since every query crossing regions would add latency and cost. The data stays in the EU, but not in Belgium. A real administration would reserve capacity in Belgium Central instead. The region has no paired region, which only matters for geo-redundant backups, and this project doesn't use them.

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
- [x] Domain model and rules
- [x] OpenAPI contract and Belgif error responses
- [ ] Applicant flow: funding calls and organisations done, grant applications and documents next
- [ ] Assessment and decision flow
- [ ] Angular frontend
- [x] CI pipeline: formatting, tests, SonarQube analysis and image publishing
- [x] Branch protection and security scanning
- [x] Container image
- [x] Shared Azure infrastructure: registry, pipeline identity and PostgreSQL server
- [x] Test environment, deployed by the pipeline on every push to develop
- [x] Staff sign-in with Microsoft Entra ID: app registration, app roles and token validation
- [ ] Acceptance and production

## Out of scope

Things a real system would need, left out to keep the project finishable:

- Citizen and organisation login through the Federal Authentication Service (eID, itsme), and verification that the applicant may represent the organisation.
- Private networking (VNet integration, private endpoints) and API Management.
- Payments of approved grants.
- Appeals against a decision.
- Email delivery. Notifications are shown in the application.
