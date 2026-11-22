using 'environment.bicep'

param environmentName = 'tst'

// Next to the PostgreSQL server, so queries never cross regions.
param location = 'francecentral'

param loadDemoData = true

param imageTag = readEnvironmentVariable('IMAGE_TAG')
param registryLoginServer = readEnvironmentVariable('REGISTRY_LOGIN_SERVER')
param postgresHost = readEnvironmentVariable('POSTGRES_HOST')
param documentsEndpoint = readEnvironmentVariable('DOCUMENTS_ENDPOINT')
param appIdentityId = readEnvironmentVariable('APP_IDENTITY_ID')
param appIdentityClientId = readEnvironmentVariable('APP_IDENTITY_CLIENT_ID')
param appIdentityPrincipalId = readEnvironmentVariable('APP_IDENTITY_PRINCIPAL_ID')
param alertActionGroupId = readEnvironmentVariable('ALERT_ACTION_GROUP_ID')

param apiClientId = readEnvironmentVariable('API_CLIENT_ID')
