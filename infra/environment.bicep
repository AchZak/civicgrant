targetScope = 'resourceGroup'

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

@description('Blob endpoint of the shared documents storage account: https://<name>.blob.core.windows.net/.')
param documentsEndpoint string

@description('Resource id of the identity the application runs with, id-civicgrant-app-<env>.')
param appIdentityId string

@description('Client id of that identity: how the app names it when it asks Entra for tokens.')
param appIdentityClientId string

@description('Object id of that identity: who receives the telemetry role.')
param appIdentityPrincipalId string

@description('Load the demo data after the schema migrations. Never in production.')
param loadDemoData bool

@description('Resource id of the shared action group the alert rules notify, ag-civicgrant-shared.')
param alertActionGroupId string

// Exactly 36 characters: an empty client id would leave the token audience unchecked.
@description('Client id of the environment\'s API registration (infra/entra.bicep): the audience every access token must carry.')
@minLength(36)
@maxLength(36)
param apiClientId string

var tags = {
  project: 'civicgrant'
  environment: environmentName
  managedBy: 'bicep'
}

var databaseName = 'civicgrant_${environmentName}'

var documentsContainer = 'documents-${environmentName}'

var databaseRole = last(split(appIdentityId, '/'))

var tokenIssuer = '${environment().authentication.loginEndpoint}${tenant().tenantId}/v2.0'

var jdbcOptions = [
  'sslmode=verify-full'

  // The driver's default looks for a root.crt the image doesn't have.
  'sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory'
  'authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin'
  'azure.managedIdentityEnabled=true'
  'azure.clientId=${appIdentityClientId}'
]

var appSettings = [
  {
    name: 'SPRING_DATASOURCE_URL'
    value: 'jdbc:postgresql://${postgresHost}/${databaseName}?${join(jdbcOptions, '&')}'
  }
  {
    name: 'SPRING_DATASOURCE_USERNAME'
    value: databaseRole
  }
  {
    // The server allows 35 connections for all environments together.
    name: 'SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE'
    value: '5'
  }
  {
    name: 'SPRING_FLYWAY_LOCATIONS'
    value: loadDemoData ? 'classpath:db/migration,classpath:db/seed' : 'classpath:db/migration'
  }
  {
    name: 'JAVA_TOOL_OPTIONS'
    value: '-javaagent:/application/applicationinsights-agent.jar'
  }
  {
    // Not a secret: Application Insights only accepts Entra-authenticated telemetry.
    name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
    value: monitoring.outputs.connectionString
  }
  {
    name: 'APPLICATIONINSIGHTS_AUTHENTICATION_STRING'
    value: 'Authorization=AAD;ClientId=${appIdentityClientId}'
  }
  {
    // JSON logs on Azure only, so they stay plain text locally and in tests.
    name: 'LOGGING_STRUCTURED_FORMAT_CONSOLE'
    value: 'ecs'
  }
  {
    name: 'SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI'
    value: tokenIssuer
  }
  {
    name: 'SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES'
    value: apiClientId
  }
  {
    name: 'CIVICGRANT_DOCUMENTS_ENDPOINT'
    value: documentsEndpoint
  }
  {
    name: 'CIVICGRANT_DOCUMENTS_CONTAINER'
    value: documentsContainer
  }
  {
    name: 'CIVICGRANT_DOCUMENTS_CLIENTID'
    value: appIdentityClientId
  }
]

module monitoring 'modules/environment/monitoring.bicep' = {
  params: {
    workspaceName: 'log-civicgrant-${environmentName}'
    applicationInsightsName: 'appi-civicgrant-${environmentName}'
    location: location
    tags: tags
    publisherIdentityId: appIdentityId
    publisherPrincipalId: appIdentityPrincipalId
  }
}

module containerAppEnvironment 'modules/environment/container-app-environment.bicep' = {
  params: {
    name: 'cae-civicgrant-${environmentName}'
    location: location
    tags: tags
    workspaceId: monitoring.outputs.workspaceId
  }
}

module containerApp 'modules/environment/container-app.bicep' = {
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

module alerts 'modules/environment/alerts.bicep' = {
  params: {
    environmentName: environmentName
    tags: tags
    applicationInsightsId: monitoring.outputs.applicationInsightsId
    actionGroupId: alertActionGroupId
  }
}

module workbook 'modules/environment/workbook.bicep' = {
  params: {
    environmentName: environmentName
    location: location
    tags: tags
    workspaceId: monitoring.outputs.workspaceId
  }
}

output appName string = containerApp.outputs.name
output appUrl string = 'https://${containerApp.outputs.fqdn}'
