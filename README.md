# CivicGrant

A grant application portal for a (fictional) Belgian public administration. Non-profit organisations apply for funding through a call for projects, a civil servant assesses each application, and a separate decision maker approves or rejects it with a written motivation.

I built this to get hands-on with Azure on a project that looks like the applications I work on day to day: a real workflow, real rules, two languages, and more than one kind of user.

## How it works

A programme manager opens a funding call, for example "Neighbourhood Projects 2026", with a budget of 50,000 euros, a maximum of 15,000 euros per application and a closing date.

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
| Programme manager | Open funding calls, with their dates and budget |

Everyone signs in with Microsoft Entra ID, which is the only user store. CivicGrant keeps no accounts and no passwords, only each user's Entra object id next to the data they own, so there's no registration step. Staff get an account in the administration's tenant and a role from an administrator, which is how workforce applications usually work, and a user without a role gets no token at all. Applicants are a deliberate simplification: here they're users of the same tenant, given the Applicant role by an administrator. A real portal would let citizens sign in on their own, through the Federal Authentication Service (eID, itsme) or a Microsoft Entra External ID tenant with self-service sign-up, where every signed-in citizen is an applicant and only staff need a role.

## Business rules

- A funding call has a unique reference of capitals, digits and hyphens. It closes after it opens and can't be opened once its closing date has passed. Its cap per application is above zero and at most its total budget.
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
| Azure Policy | Refusing any resource that breaks the project's security and region rules |
| Azure Monitor | An email alert on server errors and a monitoring workbook per environment |

All infrastructure is defined in Bicep. GitHub Actions builds one image per commit and promotes it through three environments:

| Trigger | Environment |
|---|---|
| Push to `develop` | Test |
| Push to `main` | Acceptance |
| Manual approval after acceptance | Production, with the same image |

The pipeline authenticates to Azure with workload identity federation, so there are no stored credentials anywhere, including in CI.

## Decisions

**Rolling evaluation instead of a competitive call.** Many calls for projects collect everything until the deadline, score it and fund from the top down. That needs scoring criteria, a ranking step and bulk decisions. Rolling evaluation is a legitimate model, often used for smaller grants, and keeps the scope focused on the workflow.

**No message broker.** The domain's events are handled in-process, by listeners in the same application. The audit trail writes its entry in the same transaction as the change. The business metrics, and the removal of a deleted document's file, wait until the transaction has committed. With one application and no other system consuming these events, a broker would add cost and moving parts without solving anything. I'd introduce Azure Service Bus with a transactional outbox the moment another system needs them, for example a finance system that pays out approved grants.

**Key Vault is almost empty, on purpose.** Managed Identity removes the database and storage credentials. Key Vault only holds what can't be made passwordless.

**No user table.** Users live in Entra ID. The database only stores their object id to record who did what.

**The budget can't be overspent, even by two decision makers at once.** A funding call keeps the amount already committed. An approval first checks the application's own rules, such as who may decide and whether the amount stays within what was requested, and only then takes the grant from what's left of the call's budget, so a refused approval never touches the budget. The application and the call are saved in the same transaction. The call has a version, like an application: when two approvals read the call at the same moment, the second one finds the version changed when it saves, answers 409 `concurrentUpdate` and changes nothing, and the client reads the call again and retries. Without the version, both would write their own total and one grant would silently disappear from the budget. Locking the call's row while deciding would also work, but every other decision on that call would then have to wait.

**An assessment and a decision share their application's id.** An application has at most one of each, so in the database the application's id is also the key of its assessment and of its decision, instead of a separate id with a uniqueness constraint next to it: a second one can't even be stored. They're saved with their application, because the application decides whether they may exist.

**Two people can't overwrite each other's change.** Every application has a version. Saving loads the stored row and refuses an application that was read before someone else saved it, and the database checks the version once more in the `UPDATE`, so the second of two simultaneous edits answers 409 `concurrentUpdate` instead of silently replacing the first. The client reads the application again and retries. The stored row is updated in place, never replaced by a new copy, because that's what lets JPA compare the versions.

**Ids are generated by the domain.** An aggregate has its identity before it's saved.

**Reference numbers come from one PostgreSQL sequence.** An application gets a reference people can read and quote, such as `CG-2026-000042`, when its draft is created. The number comes from a database sequence, which hands out each value once even when two applicants create a draft at the same moment, and the year from the application's clock, in UTC. The counter doesn't restart in January, so references stay unique without a table per year; six digits leave room for a million applications. A creation that's refused or rolled back still used its number, so there are gaps: the number is a label, not a count.

**Aggregates guard their own rules.** A grant application changes only through methods named after what happens, such as `submit`, `recordAssessment` or `approve`, never through setters. Each method checks who is acting, then the status and dates, then the data, and either succeeds completely or throws. An aggregate has two ways in: `create` runs the creation rules and records an event, and `restore` rebuilds it from storage without running any rule, because data that was valid when it was saved must stay readable when a rule changes later. The grant application is restored from one snapshot record, which still refuses stored data that contradicts its status, such as an approved application without a decision. Aggregates refer to each other by id. A rule that needs another aggregate receives it read-only: submitting reads the funding call's dates and cap, but only the funding call changes its own budget. Each aggregate has its own package, and ArchUnit fails the build if two of them ever depend on each other in a cycle.

**Roles are checked by the domain, and the build fails if a use case forgets.** Every use case starts by checking the caller's role, before it loads anything, so a caller without the role learns nothing about what exists. The endpoints have no role rules of their own: one place decides who may do what, and it's the place the tests already cover. A use case lists every role it accepts, never "any role", so a role added later gets nothing until someone decides it should: the programme manager, added after the others, reads funding calls but gets a 403 on organisations and applications. An ArchUnit rule fails the build if a use case doesn't make that check itself, and each use case's tests prove it checks the right role. A test calling every use case with the wrong role was the alternative, but it only catches the use cases someone remembered to add to its list.

**Funding calls are opened by a role of their own.** Opening a call sets the dates and the budget every grant is taken from. That's a programme's job, not an assessor's or a decision maker's, so it's a fourth role, `PROGRAMME_MANAGER`, which can be given on its own. The domain checks the call's rules, and the database keeps its reference unique: when two managers pick the same reference, the second gets a 409. The opening goes into the audit trail with the reference and the total budget, so it's on record who set a budget.

**One search serves applicants and staff, and the visibility rule is written once.** Applicants list their own applications; assessors and decision makers list everything except other people's drafts. The domain derives who may see what from the caller's roles, in one small type that reading a single application uses too, and the database applies it in the query, so a page never holds an application the caller couldn't open. Results come most recently changed first, then by id, so going from page to page never repeats or skips an item. The query is built with the JPA Criteria API rather than Spring Data's `Specification` support, whose interface also offers bulk updates and deletes that would bypass the version check, and it reads only the columns a list shows.

**Transactions follow the kind of request, not annotations.** The domain has no Spring annotations. Every use case takes exactly one command or one query, records that implement the domain's plain `Command` or `Query` interface, and the application wraps it accordingly: a command runs in a read-write transaction that rolls back when the domain refuses it, a query in a read-only one. The type decides, not the name, and ArchUnit fails the build if a use case takes anything else. A test registers an organisation through the real application against PostgreSQL, so wiring that lost the transaction fails the build rather than a request in production.

**Closed sets of outcomes are sealed types.** An assessment recommends `Approve(proposedAmount)` or `Reject()`, a decision is an `Approval` with a granted amount or a `Rejection` with a formal ground, and a grant application records one of eight events. Each set is a sealed interface of records, so a rejection with a granted amount can't even be built, and a `switch` over one needs no `default`: adding a ninth event breaks the build until the audit trail says how to record it.

**Failures are codes, in four families.** Every expected failure is a domain exception with a stable code, such as `SUBMISSION_AFTER_CLOSING_DATE`, which the frontend translates into Dutch or French. The English message is for logs only. The family says what kind of failure it is, and the API maps each to one HTTP status: not found (404), forbidden (403), business rule violated (400), and not allowed in the current status (409). When someone may not even know an application exists, such as an applicant asking for someone else's, or staff asking for a draft, the answer is 404 rather than 403, so ids can't be probed. A bug, such as corrupt stored data, is a plain Java exception and becomes a 500.

**Nobody decides alone, or on their own application.** Whoever assessed an application can't decide on it (`SEPARATION_OF_DUTIES`), and an applicant can't assess or decide on their own (`CONFLICT_OF_INTEREST`), even with every role. Both are domain rules, not database constraints: they compare people stored in different tables, which a `CHECK` constraint can't do. The test environment has two staff accounts, members of the tenant's own domain at no cost, so the rules are also checked there with real tokens: one assesses, the other decides, and the assessor's attempt to decide answers 403.

**The audit trail is built from domain events, and holds no free text.** Every state change records an event, and the compiler checks that each event says how it's recorded: as one audit entry, or as none when another entry already holds it. A grant taken from a call's budget, for example, is already in the approval's entry with its amount. Entries hold ids, codes and amounts, never what people typed, such as a description, a comment, a motivation or a file name: audit rows are append-only, so personal data written there could never be erased on request, as the GDPR requires. The domain publishes its events after saving, and a listener writes the entry in the same transaction as the change, so there's never a change without its history, nor history of a change that was rolled back: if the entry can't be written, the change is undone too. Each entry is about exactly one subject, an application, an organisation or a funding call, with a foreign key to it, and the database refuses a row with none or more than one. Organisations registered on the test environment before the audit was switched on have no entry. The audit only listens: ArchUnit fails the build if anything else in the domain depends on it.

**The database and the domain agree on every limit.** Each `CHECK` constraint and column length in the schema has a matching domain rule with the same bound, so a valid aggregate always fits its row, and a violated constraint means a bug rather than a user mistake. The domain also enforces what the schema can't express, such as the length of a description stored as `TEXT`. Text is measured after removing leading and trailing spaces, because that's what gets stored.

**The API follows the Belgif REST guidelines.** Belgif, the Belgian public sector's interoperability framework, publishes a guide for REST APIs, and the contract follows it: paths in lowerCamelCase (`/fundingCalls`), collections as objects with an `items` list, and pagination with `page`, `pageSize`, `total` and links to the page itself and to the previous and next ones. A field without a value, such as the submission time of a draft, is left out of the JSON rather than sent as `null`, as the contract describes it. Lifecycle steps are resources named by a noun, never a verb in the path: an applicant creates `/applications/{id}/submission` with POST, and anyone allowed can read it with GET. The same goes for the assessment and the decision, and each gets its own access rule, so the assessment stays internal while the applicant reads the decision and its motivation. A decision is sent as one flat body, an outcome with either a granted amount or a rejection ground, rather than a choice between two schemas, which the Java code generator handles poorly; when the outcome and the field don't match, the answer is a 400 naming the field. Each resource has its own tag, so each generated interface is implemented by exactly one controller.

**Every error is a Belgif problem.** Errors are Problem Details (RFC 9457) as Belgif defines them, rendered by Belgif's own library. The `type` is the identifier clients rely on and translate: standard problems such as `urn:problem-type:belgif:badRequest`, with one issue per invalid field, `resourceNotFound` or `noAccessToken`, and one business problem per domain code, such as `urn:problem-type:civicgrant:submissionAfterClosingDate`. A broken business rule answers 400, not 422, because Belgif's list of status codes has no 422. A 404 looks the same whether the item is missing or hidden, and a 500 never reveals the exception. A request body that refers to something that doesn't exist, such as an unknown funding call when creating an application, is a 400 with a `referencedResourceNotFound` issue pointing at the field, not a 404: the URL exists, the reference in the body doesn't. Spring Security rejects a token before any controller runs, so its entry point hands the problem to Spring MVC, and the library renders it like every other error: one place decides what an error looks like. Each operation in the contract lists the problems it can return, and Belgif's problem schemas are copied into the contract unchanged rather than downloaded during the build. Spring's default problems would have been one dependency fewer, but their `type` is `about:blank` and they don't say which field is wrong.

**A created resource is read back, and every link is absolute.** After a create, the API reads the new resource back and returns it, so the client sees what was stored, such as the trimmed name and the enterprise number without dots, not an echo of its own request. The `Location` header, like every link the API returns, is an absolute URL, as Belgif's guide asks. The Container Apps ingress ends HTTPS and passes plain HTTP to the app, so the app takes the original scheme and host from the ingress's `X-Forwarded-*` headers, and trusts them only from private network addresses, where the ingress connects from: a client on the internet can't fake them. Spring Boot does this by itself once it recognises the platform, but the setting is explicit, so it doesn't depend on that detection, and a test against a real server proves it.

**One image for every environment.** The image contains the application only. Everything that differs between environments, such as the database, the identity and the monitoring target, is set as environment variables on each Container App. The image that passed acceptance is exactly the one that goes to production.

**One PostgreSQL server for all environments, to keep costs low.** Each environment has its own database and its own Managed Identity, which can only reach that database. A real deployment would give each environment its own server. The server lives with the shared resources, not in an environment, so deleting and recreating an environment never touches the data, and a delete lock stops anyone removing the server by mistake. Sign-in is Microsoft Entra only: password authentication is off, so no database password exists anywhere. It's sized for the free services, with storage auto-grow off, because billing counts provisioned storage, not data.

**Database roles are bootstrapped by hand, and each database admits only its own app.** Bicep can't create PostgreSQL roles, and no database administrator credential exists in CI. So the server's Entra administrator runs `infra/bootstrap/database-role.sql` once per environment, from Azure Cloud Shell: the firewall already admits Azure, while a rule for a home IP address would stay open for good, since the delete lock blocks removing it. The script maps a role to the environment's app identity, then takes away PostgreSQL's default "every role may connect" from the environment's database and lets in that role alone. The role may create schemas there, so Flyway creates the `civicgrant` schema and the app owns everything in it, with no further grants. The script can be rerun safely and runs as one transaction that reads back its own result before committing, because PostgreSQL only warns when a grant doesn't apply.

**The app signs in to PostgreSQL with a token, through Microsoft's JDBC plugin.** On Azure, the datasource URL names Microsoft's authentication plugin. For each new connection, the plugin asks the app's managed identity, chosen by its client id, for a Microsoft Entra token and hands it to the PostgreSQL driver as the password. The same URL makes the driver check the server's certificate and host name (`verify-full`) against the Java runtime's own trust store, which already contains Azure's root certificates, so the image carries no certificate file. Locally and in tests the URL doesn't name the plugin, so Docker Compose and Testcontainers keep signing in with a password. Spring Cloud Azure would have set this up from a single property, but its current releases support Spring Boot 4.0, not the 4.1 this project runs. The plugin is what Spring Cloud Azure uses underneath, so switching later changes configuration, not behaviour. The Azure SDK's default HTTP transport, Netty, is replaced by the JDK's built-in HTTP client: one networking stack fewer in the image, and fewer security updates to follow.

**Documents live in Blob Storage, named by ids and reached with the app's identity.** One storage account sits with the shared resources, next to the database, with one private container per environment. Each environment's app identity may read and write its own container and nothing else. Shared keys are off, so on Azure no connection string exists that could leak, and nothing is ever public. Deleted files and containers stay recoverable for seven days, and a delete lock protects the account. A file is stored as `applications/{applicationId}/{documentId}.pdf`: the name the applicant gave it stays in the database as information, never in a path, so it can't be used to reach another file and puts no personal data in storage names. Locally and in tests, Azurite, Microsoft's emulator, stands in for Blob Storage. At the test environment's volumes the account costs well under a cent a month.

**A file and its row stay in step without a distributed transaction.** Attaching a document checks the rules, saves the application, which already writes the row and its audit entry to the database, and uploads the file last, still inside the transaction. A version conflict or a failed audit entry therefore fails before any file exists, and a failed upload rolls back the row and its entry. Removing a document deletes the row, and the file only after the transaction has committed, so a removal that's rolled back keeps its file. The result: a row never points to a missing file. The worst case is a file without a row, after a failed commit or a failed delete, which is private, named by ids, and logged. Cleaning those up would be a scheduled job if it ever mattered.

**The domain checks the real bytes, and uploads have a hard limit.** A document must be a PDF of at most 10 MiB: the declared type must say so, and the content must start with the PDF signature, so a renamed image is refused. The whole file reaches the use case as bytes, so the domain checks size and signature itself, without any I/O. Passing a stream and its size would have saved memory, but then a domain service would read the first bytes, push them back and handle I/O errors. The cost is at most 10 MiB in memory per upload, bounded by the server: Tomcat refuses a file over 10 MiB or a request over 11 MiB before any of the app's code runs, with Belgif's `payloadTooLarge` (413). Sonar flags any limit above 8 MB for review; this one is the product's rule, and uploads are only read once the caller is signed in. A download is sent as an attachment with the original file name, its exact length and `nosniff`, so a browser never tries to run it as anything but a PDF.

**Shared resources are bootstrapped by hand, once.** The registry, the pipeline's identity, the PostgreSQL server and each environment's identities and empty resource group are deployed from a developer machine, because the pipeline can't create the identities it logs in with. Everything inside an environment is deployed by the pipeline.

**Each environment has two identities, each allowed one job.** A deploy identity, trusted only by GitHub jobs running in that environment, may deploy into the environment's resource group and nowhere else. It may grant exactly one role there, the right to send telemetry, and only to an application identity: a condition on its role administrator assignment enforces that. An application identity runs the app and may pull images from one repository only, through a condition on its registry role. Both live with the shared resources, so deleting and recreating an environment keeps them and everything granted to them. A single "Owner" identity would have been simpler, but a compromised pipeline could then change anything in the subscription.

**Azure refuses what breaks the project's rules, through Azure Policy.** The templates follow a few rules: resources stay in the two chosen regions, storage accepts no shared keys, is never public and requires HTTPS with TLS 1.2 or later, the registry has no admin user and no anonymous pull, every Container App is HTTPS-only and has a managed identity, and Log Analytics and Application Insights only accept data sent with an Entra sign-in. One Azure Policy initiative, built from Microsoft's own definitions, turns each of them into a Deny rule. Azure then refuses any request that breaks one, whether it comes from a template, the CLI or the portal, with a message pointing here. The initiative is assigned to each CivicGrant resource group, not to the subscription, which also holds unrelated resources in other regions. The pipeline's deploy identities can't change policy or role assignments, so a pipeline can't loosen the rules it's held to: that takes an Owner deploying the shared templates by hand. A new or changed rule is first deployed in report-only mode, and only switched on once a compliance scan shows nothing it would refuse. Each rule is pinned to the major version of its definition, so Microsoft's fixes apply by themselves, but a change in what a rule checks waits until I raise the pin. Tags work the other way round: a resource without the project's tags inherits them from its resource group. That covers resources Azure creates on its own, such as Application Insights' alert action group, which a rule refusing untagged resources would one day block. The assignment adds the tags as its own managed identity, which may change tags in its resource group and nothing else, instead of the Contributor role Microsoft's definition suggests. Left out on purpose: PostgreSQL's Entra-only rule, still in preview and only able to report (the server allows Entra sign-in only anyway), and refusing PostgreSQL's public endpoint, which would need the private networking listed under Out of scope. Azure Policy costs nothing for Azure resources.

**The test environment costs nothing while nobody uses it, and holds no key.** The app runs on Azure Container Apps, in an environment with only the serverless Consumption profile, which has no fixed fee. It scales between zero and one replica on HTTP traffic: with no visitors nothing runs and nothing is billed, and the first visit waits for a cold start of under a minute. Half a vCPU and 1 GiB keep even a busy month far inside the free monthly allowance. Logs and telemetry go to one Log Analytics workspace, capped well below the free monthly volume. Neither the workspace nor Application Insights accepts keys: the app sends telemetry with its own managed identity, and the platform's logs are routed by a diagnostic setting. The Application Insights connection string grants nothing on its own.

**Every push to develop deploys itself, through a Deployment Stack.** The "Deploy to test" job logs in as the environment's deploy identity, writes a preview of the changes to the run's summary, and applies the templates as a Deployment Stack. The stack deletes any resource removed from the templates, so the environment always matches Git, and a deny assignment stops anyone, Owner included, from deleting its resources by hand. The job only goes green when the app reports healthy, which includes its token sign-in to the database, and when the new revision is the one serving: a revision that never becomes ready leaves the previous one running, so a health check alone could pass against the old image. Values shared by every environment are repository variables. The deploy identity and the app identity's ids are variables of the GitHub Environment, which take precedence in the jobs that use it.

**Logs are JSON on Azure, and each line knows its request.** On Azure, Spring Boot writes every console line as one JSON object in the Elastic Common Schema, a format it has built in, switched on by an environment variable on the Container App. Log Analytics then stores the level, the logger and the message as fields that can be filtered, instead of text that has to be parsed. On a laptop and in tests the same app writes plain text, which is easier to read. The Application Insights agent adds the request's trace id to every line written during a request, the same id Application Insights gives the request itself, so a warning in the logs leads straight to the request that caused it. Micrometer Tracing would have done the same with one more dependency.

**Business metrics count what was committed, not what was tried.** Next to the technical metrics, the app counts how many applications reach each step of the workflow (created, submitted, assessed, approved, rejected) and records every granted amount. A listener counts them only after the transaction has committed, so a refused or rolled-back approval never shows up in the numbers. They're plain Micrometer meters: the Application Insights agent already collects Spring Boot's metrics, so there's no exporter to configure. Micrometer only takes doubles, which is fine for a statistic; the budget itself stays in exact decimals in the database.

**One alert, on server errors only.** Each environment has one alert rule, which emails within about five minutes when the app has answered with a 500 or a 503 in the last 15. It counts server errors rather than Application Insights' failed requests, because those include every 4xx: a 403 or a 409 is the API refusing something by design, while a 500 is always a bug. Watching two status codes keeps the rule inside Azure Monitor's ten free time series. Every environment notifies one shared action group, whose address is set at deploy time, like the budget's, and never stored in the repository. An alert is only proven when it fires, so I deployed a temporary copy watching 401 and sent one request without a token: it fired after about five and a half minutes, resolved by itself about 25 minutes later, and was then deleted. Azure delivers nothing to an email address until it's confirmed with a one-time code, which expires 30 minutes after the action group is saved, and no API shows whether that happened: the test email failing is what gave it away.

**Each environment has its own monitoring workbook.** An Azure Monitor workbook, "CivicGrant" plus the environment's name, shows requests and errors per operation, the grant workflow with the amounts granted, and warning and error log lines next to their request. Its queries live in a JSON file in the repository and are deployed with Bicep, so a change to the dashboard is reviewed like code, and the next deployment overwrites anything changed by hand in the portal. Health probes are left out of every query, because they're almost all of the traffic. A workbook has no price of its own, and queries on the tables it reads aren't charged.

**Staff sign in with Microsoft Entra ID, and the API's registration is code.** Each environment has its own app registration, `civicgrant-api-<env>`, declared in `infra/entra.bicep` with the Microsoft Graph Bicep extension. A tenant administrator deploys it by hand, because creating applications needs tenant-wide rights that no pipeline identity should hold. With one registration per environment, a token for test is refused by production, and a role in test grants nothing in production. The API exposes one scope, `access_as_user`, which only an administrator can let a client use, and four app roles whose values are the names of the domain's roles. Its service principal requires an assignment, so Entra issues no token at all to a user without a role. Azure can't preview Microsoft Graph resources or put them in a Deployment Stack, so a change is reviewed as a Git diff and checked afterwards. Who holds which role isn't in Bicep: it changes more often than the API, and it would put user ids in a public repository. An administrator assigns users directly, following `infra/bootstrap/app-roles.md`. Assigning groups would need a paid Entra ID P1 licence. Terraform's `azuread` provider would have meant a second infrastructure tool for one file.

**The API checks every token itself, and won't start without knowing what to check.** Spring Security verifies the signature against the tenant's published keys, then the issuer, the audience (the environment's client id) and the expiry. The app's own validator then requires the `access_as_user` scope, which a token issued to an application rather than a person never carries, and the user's object id. Spring Boot silently skips the issuer or audience check when its setting is missing, so the app refuses to start instead. One class turns the token into the domain's actor: the object id identifies the user, and the app roles become domain roles. Roles the domain doesn't know are ignored. There's no session and no user lookup: every request carries its own proof. Locally, the app accepts tokens of the test registration. Its issuer and audience are committed, because they're public identifiers that the Angular app will publish in its bundle anyway.

**Main only receives releases from develop.** I push directly to `develop`. `main` accepts only a pull request from `develop`, merged with a merge commit, once the build, the tests and the image build have passed. Squash merges would make the two branches drift apart on every release. GitHub rulesets can't say which branch a pull request must come from, so a small workflow fails any pull request into `main` that doesn't come from `develop`. No approval is required: as the only developer, I can't approve my own pull requests. Neither branch can be force-pushed or deleted.

**Repository settings are code.** The rulesets are JSON files in `.github/rulesets`, and each GitHub Environment is a JSON file in `.github/environments` with the list of branches allowed to deploy to it next to it. A script applies them together with the security settings and can be rerun safely: it creates what's missing, replaces what changed, and removes branches no longer listed. A change to how the repository is protected is reviewed like any other change, instead of being clicked in a settings page. The test environment admits `develop` only, so a workflow on any other branch can't obtain the test deploy identity's token, even if it edits its own workflow file.

**Security checks on every change.** Secret scanning blocks a push that contains a credential, CodeQL analyses the Java code and the workflows, and Dependabot alerts report vulnerable dependencies. Renovate stays the only tool that opens update pull requests, so Dependabot only reports. Vulnerabilities can be reported privately, as `SECURITY.md` explains.

**Hosted in Belgium Central, with the database in France Central.** I chose Belgium Central so a Belgian administration's data would stay at rest in Belgium, at the same price as West Europe. When I created the database, Belgium Central had no capacity left for its size, twice. So the database runs in France Central, with each environment's application next to it, since every query crossing regions would add latency and cost. I tried again before adding Blob Storage, and Belgium Central still had no capacity, so the documents' storage account is in France Central too. The data stays in the EU, but not in Belgium. A real administration would reserve capacity in Belgium Central instead. The region has no paired region, which only matters for geo-redundant backups, and this project doesn't use them.

**A schema, not tablespaces.** Azure Database for PostgreSQL doesn't support custom tablespaces, and physical storage isn't a migration's concern anyway. Everything lives in the `civicgrant` schema.

## Running it locally

Requirements: JDK 25 and Docker.

```bash
./mvnw clean install
./mvnw -pl starter spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile starts PostgreSQL and Azurite through `compose.yaml`, keeps their data between runs and loads demo data from `db/seed`. Tests don't use it: they run against a throwaway PostgreSQL and Azurite started by Testcontainers.

## Status

- [x] Database schema
- [x] Multi-module structure
- [x] Domain model and rules
- [x] OpenAPI contract and Belgif error responses
- [x] Applicant flow: funding calls, organisations, grant applications, supporting documents and submission
- [x] Assessment and decision flow
- [x] Funding calls opened by a programme manager
- [ ] Angular frontend
- [x] CI pipeline: formatting, tests, SonarQube analysis and image publishing
- [x] Branch protection and security scanning
- [x] Container image
- [x] Shared Azure infrastructure: registry, pipeline identity and PostgreSQL server
- [x] Test environment, deployed by the pipeline on every push to develop
- [x] Staff sign-in with Microsoft Entra ID: app registration, app roles and token validation
- [x] Azure Policy guardrails on every CivicGrant resource group
- [x] Observability: JSON logs with trace ids, business metrics, an alert on server errors and a monitoring workbook
- [ ] Acceptance and production

## Out of scope

Things a real system would need, left out to keep the project finishable:

- Citizen and organisation login through the Federal Authentication Service (eID, itsme), and verification that the applicant may represent the organisation.
- Private networking (VNet integration, private endpoints) and API Management.
- Payments of approved grants.
- Appeals against a decision.
- Notifications, by email or in the application. Applicants follow the status of their application in the portal.
