@description('Globally unique registry name, letters and digits only.')
param name string

param location string
param tags object

@description('Principals allowed to push images.')
param pushPrincipalIds array = []

var acrPushRoleId = '8311e382-0749-4cb8-b61a-304f252e45ec'

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
  }
}

resource pushRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in pushPrincipalIds: {
    name: guid(registry.id, principalId, acrPushRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPushRoleId)
      principalId: principalId
      principalType: 'ServicePrincipal'
    }
  }
]

output name string = registry.name
output loginServer string = registry.properties.loginServer
