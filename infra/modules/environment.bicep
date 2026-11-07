// ============================================================
// MODULE: ENVIRONMENT FOUNDATIONS
// ============================================================
//
// Creates what one environment (tst, acc or prd) needs before its pipeline can
// deploy it:
//
//   rg-civicgrant-<env>             empty resource group, filled later by the
//                                   environment's Deployment Stack
//   in rg-civicgrant-shared         the deploy and app identities
//                                   (environment-identities.bicep)
//   on rg-civicgrant-<env>          the deploy identity's roles
//                                   (environment-access.bicep)
//
// The resource group is created here, by hand, so the pipeline never needs the
// right to create resource groups: that right covers the whole subscription,
// rg-civicgrant-shared included.


// ------------------------------------------------------------
// SCOPE
// ------------------------------------------------------------

// Creating a resource group happens at subscription level.
targetScope = 'subscription'


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Azure region of the environment: next to its database.')
param location string

@description('Tags applied to the environment\'s resources.')
param tags object

@description('Name of the shared resource group, where the identities live.')
param sharedResourceGroupName string

@description('Start of the GitHub OIDC subject claim for the repository, without the trailing colon.')
param githubSubjectPrefix string

@description('Name of the shared container registry.')
param registryName string

@description('Repository in the registry the app may pull from.')
param imageRepository string


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// The identity names, written once here and passed to both modules below.
var deployIdentityName = 'id-civicgrant-deploy-${environmentName}'
var appIdentityName = 'id-civicgrant-app-${environmentName}'

// The deploy identity's full resource id, computed from its name instead of read
// from the identity once it exists. what-if can only preview a role assignment whose
// name it can compute in advance, and these names are derived from this id.
// Subscription id first: at subscription scope, resourceId() reads a lone first
// argument as a subscription id, not as a resource group name.
var deployIdentityId = resourceId(subscription().subscriptionId, sharedResourceGroupName, 'Microsoft.ManagedIdentity/userAssignedIdentities', deployIdentityName)


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The environment's resource group, empty for now.
//    Its content is managed by a Deployment Stack, so the stack can delete
//    everything it created when the environment is torn down. The group itself
//    stays, and an empty resource group costs nothing.
resource environmentGroup 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: 'rg-civicgrant-${environmentName}'
  location: location
  tags: tags
}

// 2. The identities, in the shared resource group.
module identities 'environment-identities.bicep' = {
  scope: resourceGroup(sharedResourceGroupName)
  params: {
    environmentName: environmentName
    deployIdentityName: deployIdentityName
    appIdentityName: appIdentityName
    location: location
    tags: tags
    githubSubjectPrefix: githubSubjectPrefix
    registryName: registryName
    imageRepository: imageRepository
  }
}

// 3. The deploy identity's roles, on the environment's resource group only.
module access 'environment-access.bicep' = {
  scope: environmentGroup
  params: {
    deployIdentityId: deployIdentityId
    deployPrincipalId: identities.outputs.deployPrincipalId
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output resourceGroupName string = environmentGroup.name
output deployClientId string = identities.outputs.deployClientId
output appClientId string = identities.outputs.appClientId
output appPrincipalId string = identities.outputs.appPrincipalId
output appIdentityId string = identities.outputs.appIdentityId
