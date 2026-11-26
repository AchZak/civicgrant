targetScope = 'subscription'

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

@description('Start of the GitHub OIDC subject claim for the frontend\'s repository, without the trailing colon.')
param frontendGithubSubjectPrefix string

@description('Azure region of the environment\'s Static Web App.')
param frontendLocation string

@description('Name of the shared container registry.')
param registryName string

@description('Repository in the registry the app may pull from.')
param imageRepository string

@description('Resource id of the guardrails initiative.')
param guardrailsId string

@description('Default enforces the guardrails; DoNotEnforce only reports on them.')
@allowed([
  'Default'
  'DoNotEnforce'
])
param guardrailsEnforcementMode string

@description('Name of the shared action group the environment\'s alert rules notify.')
param alertActionGroupName string

var deployIdentityName = 'id-civicgrant-deploy-${environmentName}'
var appIdentityName = 'id-civicgrant-app-${environmentName}'
var frontendDeployIdentityName = 'id-civicgrant-frontend-${environmentName}'

// Computed from the name so what-if can preview the role assignments seeded with it.
// Subscription id first: at this scope resourceId() reads a lone first argument as one.
var deployIdentityId = resourceId(subscription().subscriptionId, sharedResourceGroupName, 'Microsoft.ManagedIdentity/userAssignedIdentities', deployIdentityName)
var frontendDeployIdentityId = resourceId(subscription().subscriptionId, sharedResourceGroupName, 'Microsoft.ManagedIdentity/userAssignedIdentities', frontendDeployIdentityName)

resource environmentGroup 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: 'rg-civicgrant-${environmentName}'
  location: location
  tags: tags
}

module identities 'environment-identities.bicep' = {
  scope: resourceGroup(sharedResourceGroupName)
  params: {
    environmentName: environmentName
    deployIdentityName: deployIdentityName
    appIdentityName: appIdentityName
    frontendDeployIdentityName: frontendDeployIdentityName
    location: location
    tags: tags
    githubSubjectPrefix: githubSubjectPrefix
    frontendGithubSubjectPrefix: frontendGithubSubjectPrefix
    registryName: registryName
    imageRepository: imageRepository
    alertActionGroupName: alertActionGroupName
  }
}

module access 'environment-access.bicep' = {
  scope: environmentGroup
  params: {
    deployIdentityId: deployIdentityId
    deployPrincipalId: identities.outputs.deployPrincipalId
  }
}

module guardrails 'guardrails-assignment.bicep' = {
  scope: environmentGroup
  params: {
    policySetDefinitionId: guardrailsId
    location: location
    enforcementMode: guardrailsEnforcementMode
  }
}

module staticWebAppsExemption 'static-web-apps-exemption.bicep' = {
  scope: environmentGroup
  dependsOn: [
    guardrails
  ]
}

// Policy is checked before a deployment starts, so the exemption must exist by then.
module frontendHosting 'frontend-hosting.bicep' = {
  scope: environmentGroup
  params: {
    name: 'stapp-civicgrant-${environmentName}'
    location: frontendLocation
    tags: tags
    deployIdentityId: frontendDeployIdentityId
    deployPrincipalId: identities.outputs.frontendDeployPrincipalId
  }
  dependsOn: [
    staticWebAppsExemption
  ]
}

output resourceGroupName string = environmentGroup.name
output deployClientId string = identities.outputs.deployClientId
output frontendDeployClientId string = identities.outputs.frontendDeployClientId
output frontendName string = frontendHosting.outputs.name
output frontendHostname string = frontendHosting.outputs.hostname
output appClientId string = identities.outputs.appClientId
output appPrincipalId string = identities.outputs.appPrincipalId
output appIdentityId string = identities.outputs.appIdentityId
