targetScope = 'subscription'

@description('Azure region for the shared resources.')
param location string

@description('GitHub repository allowed to push images, as owner/name.')
param githubRepository string

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

resource resourceGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
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
    githubRepository: githubRepository
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
