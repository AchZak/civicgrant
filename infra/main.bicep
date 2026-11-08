// ============================================================
// ENVIRONMENT TEMPLATE
// ============================================================
//
// Creates what one environment (tst, acc or prd) runs on, inside its resource
// group rg-civicgrant-<env>:
//
//   log-civicgrant-<env>     Log Analytics workspace: stores logs and telemetry
//   appi-civicgrant-<env>    Application Insights: the app's requests, errors and traces
//     role assignment        the app identity may send telemetry
//   cae-civicgrant-<env>     Container Apps environment: where the app runs
//     diagnostic setting     its console and system logs go to the workspace
//   ca-civicgrant-<env>      Container App: the application image, running
//
// Everything it builds on already exists, created by shared.bicep: the resource
// group, the identities, the registry and the database. Their values arrive as
// parameters, so whoever deploys this template needs no rights outside the
// resource group.
//
// Deployed as a Deployment Stack by the pipeline's "Deploy to test" job.
// From your machine, with the variables tst.bicepparam reads already set:
//
//   az bicep lint --file infra/main.bicep
//   az deployment group what-if --resource-group rg-civicgrant-tst --parameters infra/tst.bicepparam


// ------------------------------------------------------------
// SCOPE
// ------------------------------------------------------------

// Deployed into an existing resource group: rg-civicgrant-<env>, created by shared.bicep.
targetScope = 'resourceGroup'


// ------------------------------------------------------------
// PARAMETERS
// Values come from <env>.bicepparam.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Azure region of the environment: the region of its database.')
param location string

@description('Login server of the shared container registry: <name>.azurecr.io.')
param registryLoginServer string

@description('Tag of the image to run: the full SHA of the commit it was built from.')
param imageTag string

@description('Host name of the shared PostgreSQL server.')
param postgresHost string

@description('Resource id of the identity the application runs with, id-civicgrant-app-<env>.')
param appIdentityId string

@description('Client id of that identity: how the app names it when it asks Entra for tokens.')
param appIdentityClientId string

@description('Object id of that identity: who receives the telemetry role.')
param appIdentityPrincipalId string

@description('Load the demo data after the schema migrations. Never in production.')
param loadDemoData bool


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Applied to every resource, like the shared ones.
var tags = {
  project: 'civicgrant'
  environment: environmentName
  managedBy: 'bicep'
}

// The environment's database on the shared server, created by shared.bicep.
var databaseName = 'civicgrant_${environmentName}'

// The database role has the identity's name (see infra/bootstrap). The name is the
// last segment of the identity's resource id.
var databaseRole = last(split(appIdentityId, '/'))

// Options of the JDBC URL, one per line.
var jdbcOptions = [
  // Encrypt, check the server's certificate chain, and check that the certificate
  // was issued for this host name.
  'sslmode=verify-full'

  // Check the certificate against the Java runtime's trust store, which already
  // contains Azure's root certificates. The driver's default would look for a
  // root.crt file that the image doesn't have.
  'sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory'

  // Instead of a password, the driver asks Microsoft's plugin for one: a Microsoft
  // Entra token, obtained by the managed identity below.
  'authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin'

  // Which identity: this user-assigned managed identity, directly, without trying
  // other ways of signing in first.
  'azure.managedIdentityEnabled=true'
  'azure.clientId=${appIdentityClientId}'
]

// Spring Boot reads these environment variables as configuration properties:
// SPRING_DATASOURCE_URL is spring.datasource.url, and so on.
var appSettings = [
  {
    name: 'SPRING_DATASOURCE_URL'
    value: 'jdbc:postgresql://${postgresHost}/${databaseName}?${join(jdbcOptions, '&')}'
  }
  {
    name: 'SPRING_DATASOURCE_USERNAME'
    value: databaseRole  // No password variable: the plugin above supplies a token instead.
  }
  {
    // At most 5 connections per replica. The server allows 35 for every
    // environment together, and the bootstrap needs a few too.
    name: 'SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE'
    value: '5'
  }
  {
    // Where Flyway looks for migrations. db/seed holds the demo data.
    name: 'SPRING_FLYWAY_LOCATIONS'
    value: loadDemoData ? 'classpath:db/migration,classpath:db/seed' : 'classpath:db/migration'
  }
  {
    // Starts the Application Insights agent the Dockerfile downloads. The JVM reads
    // JAVA_TOOL_OPTIONS by itself, so the image's command stays unchanged.
    name: 'JAVA_TOOL_OPTIONS'
    value: '-javaagent:/application/applicationinsights-agent.jar'
  }
  {
    // Where the agent sends telemetry. Not a secret here: the resource accepts only
    // Entra-authenticated telemetry, so the key inside it is useless on its own.
    name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
    value: monitoring.outputs.connectionString
  }
  {
    // How the agent authenticates: an Entra token from the same managed identity.
    name: 'APPLICATIONINSIGHTS_AUTHENTICATION_STRING'
    value: 'Authorization=AAD;ClientId=${appIdentityClientId}'
  }
]


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. Where logs and telemetry go, and the app identity's right to send telemetry.
module monitoring 'modules/monitoring.bicep' = {
  params: {
    workspaceName: 'log-civicgrant-${environmentName}'
    applicationInsightsName: 'appi-civicgrant-${environmentName}'
    location: location
    tags: tags
    publisherIdentityId: appIdentityId
    publisherPrincipalId: appIdentityPrincipalId
  }
}

// 2. The Container Apps environment, sending its logs to the workspace from step 1.
module containerAppEnvironment 'modules/container-app-environment.bicep' = {
  params: {
    name: 'cae-civicgrant-${environmentName}'
    location: location
    tags: tags
    workspaceId: monitoring.outputs.workspaceId
  }
}

// 3. The application itself, in the environment from step 2.
module containerApp 'modules/container-app.bicep' = {
  params: {
    name: 'ca-civicgrant-${environmentName}'
    location: location
    tags: tags
    environmentId: containerAppEnvironment.outputs.id
    image: '${registryLoginServer}/civicgrant:${imageTag}'
    registryLoginServer: registryLoginServer
    identityId: appIdentityId
    environmentVariables: appSettings
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output appUrl string = 'https://${containerApp.outputs.fqdn}'  // The pipeline checks the app's health here.
