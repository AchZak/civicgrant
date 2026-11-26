using 'shared.bicep'

param location = 'belgiumcentral'

// The names are part of the subject too: after a rename, copy sub_claim_prefix again from
// gh api repos/AchZak/civicgrant-backend/actions/oidc/customization/sub
param githubSubjectPrefix = 'repo:AchZak@140258958/civicgrant-backend@1422522301'

// Copied the same way, from gh api repos/AchZak/civicgrant-frontend/actions/oidc/customization/sub
param frontendGithubSubjectPrefix = 'repo:AchZak@140258958/civicgrant-frontend@1469912800'

// Static Web Apps aren't offered in Belgium or France Central, and West Europe refused this
// subscription in November 2026. Only the site's metadata lives there.
param frontendLocation = 'eastus2'

param publishBranches = [
  'develop'
  'main'
]

// Standard is in the 12-month free services, Basic isn't. Switch before they end.
param registrySku = 'Standard'

param budgetAlertEmail = readEnvironmentVariable('BUDGET_ALERT_EMAIL')

// Same major as compose.yaml and the tests.
param postgresVersion = '18'

// Belgium Central had no capacity for B1ms in November 2026.
param postgresLocation = 'francecentral'

param environmentNames = [
  'tst'
]

param guardrailsEnforcementMode = 'Default'
