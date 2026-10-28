@description('Globally unique registry name, letters and digits only.')
param name string

@description('Azure region of the registry.')
param location string

@description('Tags applied to the registry.')
param tags object

@description('Principals allowed to push images.')
param pushPrincipalIds array = []

var repositoryWriterRoleId = '2a1e307c-b015-4ebd-883e-5b7698a07328'
var readerRoleId = 'acdd72a7-3385-48ef-bd42-f606fba81ae7'

resource registry 'Microsoft.ContainerRegistry/registries@2025-11-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
    roleAssignmentMode: 'AbacRepositoryPermissions'
  }
}

resource pushRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in pushPrincipalIds: {
    name: guid(registry.id, principalId, repositoryWriterRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryWriterRoleId)
      principalId: principalId
      principalType: 'ServicePrincipal'
    }
  }
]

// Writer only covers the data plane; az acr login also needs to read the registry resource.
resource readRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in pushPrincipalIds: {
    name: guid(registry.id, principalId, readerRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', readerRoleId)
      principalId: principalId
      principalType: 'ServicePrincipal'
    }
  }
]

output name string = registry.name
output loginServer string = registry.properties.loginServer
