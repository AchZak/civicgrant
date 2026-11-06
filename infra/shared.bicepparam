// ============================================================
// PARAMETER VALUES FOR shared.bicep
// ============================================================
//
// "using" names the template these values belong to, so the deploy command
// only needs --parameters infra/shared.bicepparam, not --template-file.

using 'shared.bicep'

// Region of every shared resource. Belgium Central keeps the data in Belgium.
param location = 'belgiumcentral'

// Identifies this repository in the tokens GitHub signs for its workflow runs.
// Format: repo:<owner>@<owner id>/<repository>@<repository id>.
// The ids keep the subject valid if the owner or the repository is ever renamed.
param githubSubjectPrefix = 'repo:AchZak@140258958/civicgrant@1422522301'

// One federated credential per branch: pushes to these branches may log in and
// push images. Pull requests never can.
param publishBranches = [
  'develop'
  'main'
]

// Standard is in the 12-month free services, Basic isn't.
// Switch to Basic before the free services end.
param registrySku = 'Standard'

// Read from your shell when you deploy, so the address never lands in this public
// repository. If the variable isn't set, the deployment stops with an error
// instead of creating alerts that go nowhere.
param budgetAlertEmail = readEnvironmentVariable('BUDGET_ALERT_EMAIL')

// The same major version as compose.yaml and Testcontainers, so what passes the
// tests is what runs in Azure. A major upgrade is a deliberate change, here first.
param postgresVersion = '18'

// France Central (Paris), not Belgium Central: Belgium Central had no capacity for
// the B1ms size in November 2026 (RegionalAllocationFailed, twice). Still in the EU,
// close to Brussels, and the free services aren't tied to a region. Each
// environment's app runs here too, next to its database.
param postgresLocation = 'francecentral'

// One database per environment on the shared server. Add civicgrant_acc and
// civicgrant_prd when those environments are built.
param postgresDatabaseNames = [
  'civicgrant_tst'
]
