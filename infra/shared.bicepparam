using 'shared.bicep'

param location = 'belgiumcentral'

// The ids keep the subject valid if the owner or the repository is renamed.
param githubSubjectPrefix = 'repo:AchZak@140258958/civicgrant@1422522301'

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
