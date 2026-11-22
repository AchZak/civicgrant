@description('Globally unique registry name, letters and digits only.')
param name string

@description('Azure region of the registry.')
param location string

@description('Tags applied to the registry.')
param tags object

@description('Registry SKU: Basic or Standard.')
param sku string

@description('Principals allowed to push images.')
param pushPrincipalIds array = []

@description('Entra user object ids allowed to list repositories and read images, without changing anything.')
param viewerUserIds array = []

var repositoryWriterRoleId = '2a1e307c-b015-4ebd-883e-5b7698a07328'
var repositoryReaderRoleId = 'b93aa761-3e63-49ed-ac28-beffa264f7ac'
var catalogListerRoleId = 'bfdb9389-c9a5-478a-bb2f-ba9ca092c3c7'
var readerRoleId = 'acdd72a7-3385-48ef-bd42-f606fba81ae7'

resource registry 'Microsoft.ContainerRegistry/registries@2025-11-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: sku
  }
  properties: {
    adminUserEnabled: false

    // ABAC mode: AcrPull and AcrPush have no effect, only the Repository roles count.
    roleAssignmentMode: 'AbacRepositoryPermissions'

    // Azure's defaults, declared so what-if stays clean.
    dataEndpointEnabled: false
    encryption: {
      status: 'disabled'
    }
    networkRuleBypassAllowedForTasks: false
  }
}

resource pushRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in pushPrincipalIds: {
    name: guid(registry.id, principalId, repositoryWriterRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryWriterRoleId)
      principalId: principalId

      // Skips a lookup that fails for an identity Entra hasn't replicated yet.
      principalType: 'ServicePrincipal'
    }
  }
]

// az acr login also reads the registry resource, which Writer doesn't cover.
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

// In ABAC mode even an Owner can't see the images without this role.
resource viewerReadRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for userId in viewerUserIds: {
    name: guid(registry.id, userId, repositoryReaderRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryReaderRoleId)
      principalId: userId
      principalType: 'User'
    }
  }
]

resource viewerListRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for userId in viewerUserIds: {
    name: guid(registry.id, userId, catalogListerRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', catalogListerRoleId)
      principalId: userId
      principalType: 'User'
    }
  }
]

output name string = registry.name
output loginServer string = registry.properties.loginServer
