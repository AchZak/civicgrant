targetScope = 'subscription'

@description('Azure region for the shared resources.')
param location string

@description('GitHub OIDC subject prefix of the repository, as shown under Settings > Actions > OIDC: repo:owner/name, or repo:owner@id/name@id for repositories created or renamed since July 2026.')
param githubSubjectPrefix string

@description('Branches whose workflow runs may push images.')
param publishBranches array = [
  'develop'
  'main'
]

var tags = {
  project: 'civicgrant'
  environment: 'shared'
  managedBy: 'bicep'
}

resource resourceGroup 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: 'rg-civicgrant-shared'
  location: location
  tags: tags
}

module pipelineIdentity 'modules/pipeline-identity.bicep' = {
  scope: resourceGroup
  params: {
    name: 'id-civicgrant-pipeline'
    location: location
    tags: tags
    githubSubjectPrefix: githubSubjectPrefix
    branches: publishBranches
  }
}

module registry 'modules/container-registry.bicep' = {
  scope: resourceGroup
  params: {
    name: 'crcivicgrant${uniqueString(subscription().subscriptionId)}'
    location: location
    tags: tags
    pushPrincipalIds: [
      pipelineIdentity.outputs.principalId
    ]
  }
}

output registryName string = registry.outputs.name
output registryLoginServer string = registry.outputs.loginServer
output pipelineClientId string = pipelineIdentity.outputs.clientId
output tenantId string = tenant().tenantId
output subscriptionId string = subscription().subscriptionId
