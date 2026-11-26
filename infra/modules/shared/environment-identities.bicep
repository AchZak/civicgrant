@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Name of the identity GitHub Actions deploys the environment with.')
param deployIdentityName string

@description('Name of the identity the application runs with.')
param appIdentityName string

@description('Name of the identity the frontend\'s workflow deploys the Static Web App with.')
param frontendDeployIdentityName string

@description('Azure region of the identities: the environment\'s region.')
param location string

@description('Tags applied to the identities.')
param tags object

@description('Start of the GitHub OIDC subject claim for the repository, without the trailing colon.')
param githubSubjectPrefix string

@description('Start of the GitHub OIDC subject claim for the frontend\'s repository, without the trailing colon.')
param frontendGithubSubjectPrefix string

@description('Name of the shared container registry.')
param registryName string

@description('Repository in the registry the app may pull from.')
param imageRepository string

@description('Name of the shared action group the environment\'s alert rules notify.')
param alertActionGroupName string

var repositoryReaderRoleId = 'b93aa761-3e63-49ed-ac28-beffa264f7ac'
var managedIdentityOperatorRoleId = 'f1a07417-d97a-45cb-824c-7a7467783830'
var readerRoleId = 'acdd72a7-3385-48ef-bd42-f606fba81ae7'

// Any read must target imageRepository. The role allows nothing else.
var repositoryCondition = '((!(ActionMatches{\'Microsoft.ContainerRegistry/registries/repositories/content/read\'}) AND !(ActionMatches{\'Microsoft.ContainerRegistry/registries/repositories/metadata/read\'})) OR (@Request[Microsoft.ContainerRegistry/registries/repositories:name] StringEqualsIgnoreCase \'${imageRepository}\'))'

resource deployIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: deployIdentityName
  location: location
  tags: tags
  properties: {
    isolationScope: 'None'
  }
}

// A job with "environment: tst" gets the subject <prefix>:environment:tst, not its branch.
resource deployCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2024-11-30' = {
  parent: deployIdentity
  name: 'github-environment-${environmentName}'
  properties: {
    issuer: 'https://token.actions.githubusercontent.com'
    subject: '${githubSubjectPrefix}:environment:${environmentName}'
    audiences: [
      'api://AzureADTokenExchange'
    ]
  }
}

resource frontendDeployIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: frontendDeployIdentityName
  location: location
  tags: tags
  properties: {
    isolationScope: 'None'
  }
}

resource frontendDeployCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2024-11-30' = {
  parent: frontendDeployIdentity
  name: 'github-environment-${environmentName}'
  properties: {
    issuer: 'https://token.actions.githubusercontent.com'
    subject: '${frontendGithubSubjectPrefix}:environment:${environmentName}'
    audiences: [
      'api://AzureADTokenExchange'
    ]
  }
}

resource appIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: appIdentityName
  location: location
  tags: tags
  properties: {
    isolationScope: 'None'
  }
}

resource registry 'Microsoft.ContainerRegistry/registries@2025-11-01' existing = {
  name: registryName
}

resource appPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, appIdentity.id, repositoryReaderRoleId)
  scope: registry
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryReaderRoleId)
    principalId: appIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    condition: repositoryCondition
    conditionVersion: '2.0'
    description: 'Pull ${imageRepository} images only.'
  }
}

resource deployAssignsAppIdentity 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(appIdentity.id, deployIdentity.id, managedIdentityOperatorRoleId)
  scope: appIdentity
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', managedIdentityOperatorRoleId)
    principalId: deployIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource alertActionGroup 'Microsoft.Insights/actionGroups@2023-01-01' existing = {
  name: alertActionGroupName
}

// An alert rule can only name an action group its creator can read.
resource deployReadsActionGroup 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(alertActionGroup.id, deployIdentity.id, readerRoleId)
  scope: alertActionGroup
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', readerRoleId)
    principalId: deployIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

output deployPrincipalId string = deployIdentity.properties.principalId
output deployClientId string = deployIdentity.properties.clientId
output frontendDeployPrincipalId string = frontendDeployIdentity.properties.principalId
output frontendDeployClientId string = frontendDeployIdentity.properties.clientId
output appClientId string = appIdentity.properties.clientId
output appPrincipalId string = appIdentity.properties.principalId
output appIdentityId string = appIdentity.id
