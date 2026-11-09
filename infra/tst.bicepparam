// ============================================================
// PARAMETER VALUES FOR main.bicep, TEST ENVIRONMENT
// ============================================================
//
// "using" names the template these values belong to, so the deploy command
// only needs --parameters infra/tst.bicepparam.

using 'main.bicep'

param environmentName = 'tst'

// France Central, next to the shared PostgreSQL server (postgresLocation in
// shared.bicepparam): the app's queries never cross regions.
param location = 'francecentral'

// The test environment shows the demo funding calls and applications.
param loadDemoData = true

// The values below are read from your shell when you deploy, the same way as the
// budget email in shared.bicepparam. If one isn't set, the deployment stops with an
// error instead of deploying an empty value.
//   In the pipeline:  GitHub Environment variables of tst, and the commit SHA.
//   On your machine:  the outputs of the shared deployment (az deployment sub show).
// None of them is a secret, but keeping them out of the repository means nothing
// has to be copied here by hand, and acc and prd read the same variable names
// from their own GitHub Environments.
param imageTag = readEnvironmentVariable('IMAGE_TAG')
param registryLoginServer = readEnvironmentVariable('REGISTRY_LOGIN_SERVER')
param postgresHost = readEnvironmentVariable('POSTGRES_HOST')
param appIdentityId = readEnvironmentVariable('APP_IDENTITY_ID')
param appIdentityClientId = readEnvironmentVariable('APP_IDENTITY_CLIENT_ID')
param appIdentityPrincipalId = readEnvironmentVariable('APP_IDENTITY_PRINCIPAL_ID')

// The client id of civicgrant-api-tst, from the outputs of infra/entra.bicep
// (az deployment group show --resource-group rg-civicgrant-shared --name civicgrant-entra).
param apiClientId = readEnvironmentVariable('API_CLIENT_ID')
