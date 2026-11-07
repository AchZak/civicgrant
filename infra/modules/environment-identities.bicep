// ============================================================
// MODULE: ENVIRONMENT IDENTITIES
// ============================================================
//
// Creates the two identities of one environment (tst, acc or prd), and their
// rights on shared resources:
//
//   id-civicgrant-deploy-<env>   GitHub Actions deploys the environment with it
//   id-civicgrant-app-<env>      the application runs with it
//
// Both live in rg-civicgrant-shared, not in the environment's resource group:
// deleting and recreating the environment keeps them, so their role assignments
// and the app's PostgreSQL role stay valid.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by environment.bicep.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Name of the identity GitHub Actions deploys the environment with.')
param deployIdentityName string

@description('Name of the identity the application runs with.')
param appIdentityName string

@description('Azure region of the identities: the environment\'s region.')
param location string

@description('Tags applied to the identities.')
param tags object

@description('Start of the GitHub OIDC subject claim for the repository, without the trailing colon.')
param githubSubjectPrefix string

@description('Name of the shared container registry.')
param registryName string

@description('Repository in the registry the app may pull from.')
param imageRepository string


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Ids of built-in Azure roles, the same in every tenant.
// To look one up: az role definition list --name '<role name>' --query "[0].name" --output tsv
var repositoryReaderRoleId = 'b93aa761-3e63-49ed-ac28-beffa264f7ac'        // Container Registry Repository Reader
var managedIdentityOperatorRoleId = 'f1a07417-d97a-45cb-824c-7a7467783830' // Managed Identity Operator

// The app may read images from one repository only. An ABAC condition narrows a role
// assignment: the role says what (read images), the condition says where.
// How to read it: "if the request is a content or metadata read, then the repository
// must be imageRepository". Any other request isn't constrained, but the role allows
// no other request anyway. Microsoft's syntax, from the ACR ABAC documentation.
var repositoryCondition = '((!(ActionMatches{\'Microsoft.ContainerRegistry/registries/repositories/content/read\'}) AND !(ActionMatches{\'Microsoft.ContainerRegistry/registries/repositories/metadata/read\'})) OR (@Request[Microsoft.ContainerRegistry/registries/repositories:name] StringEqualsIgnoreCase \'${imageRepository}\'))'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The deploy identity.
//    isolationScope None, Azure's default: the identity may be used from any region.
resource deployIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: deployIdentityName
  location: location
  tags: tags
  properties: {
    isolationScope: 'None'
  }
}

// 2. Its federated credential: GitHub jobs that run in the GitHub Environment
//    of the same name may log in with it.
//    A job with "environment: tst" gets the subject <prefix>:environment:tst, not
//    <prefix>:ref:refs/heads/develop. The GitHub Environment itself only accepts
//    jobs from develop (its deployment branch policy), so both rules apply.
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

// 3. The app identity: attached to the environment's Container App, it pulls the
//    image and signs in to PostgreSQL. No federated credential: only Azure uses it.
resource appIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: appIdentityName
  location: location
  tags: tags
  properties: {
    // None, not Regional: the identity sits in the environment's region, but None
    // keeps attaching it possible if the app ever moves region.
    isolationScope: 'None'
  }
}

// The shared registry, by name: "existing" references a resource without deploying it.
resource registry 'Microsoft.ContainerRegistry/registries@2025-11-01' existing = {
  name: registryName
}

// 4. The app may pull images: Container Registry Repository Reader, limited by the
//    condition above to one repository.
resource appPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, appIdentity.id, repositoryReaderRoleId)
  scope: registry
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryReaderRoleId)
    principalId: appIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    condition: repositoryCondition
    conditionVersion: '2.0'  // The condition language version every current condition uses.
    description: 'Pull ${imageRepository} images only.'
  }
}

// 5. The deploy identity may attach the app identity to a Container App.
//    Attaching an identity to a resource needs the right to "assign" it
//    (Microsoft.ManagedIdentity/userAssignedIdentities/assign/action).
//    Managed Identity Operator grants that, scoped here to this one identity.
resource deployAssignsAppIdentity 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(appIdentity.id, deployIdentity.id, managedIdentityOperatorRoleId)
  scope: appIdentity
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', managedIdentityOperatorRoleId)
    principalId: deployIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output deployPrincipalId string = deployIdentity.properties.principalId // For the role assignments on the environment.
output deployClientId string = deployIdentity.properties.clientId       // AZURE_CLIENT_ID of the GitHub Environment.
output appClientId string = appIdentity.properties.clientId             // How the app names its identity at runtime.
output appPrincipalId string = appIdentity.properties.principalId       // For the PostgreSQL role (bootstrap).
output appIdentityId string = appIdentity.id                            // Attached to the Container App.
