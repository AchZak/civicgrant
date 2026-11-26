@description('Name of the Static Web App.')
param name string

@description('Azure region of the Static Web App: where its metadata lives, the content is served worldwide.')
param location string

@description('Tags applied to the Static Web App.')
param tags object

@description('Resource id of the identity the frontend\'s workflow deploys with. Seeds the role assignment name.')
param deployIdentityId string

@description('Object id of that identity.')
param deployPrincipalId string

var contributorRoleId = 'b24988ac-6180-42a0-ab88-20f7382dd24c'

resource staticWebApp 'Microsoft.Web/staticSites@2025-03-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {
    stagingEnvironmentPolicy: 'Disabled'
    allowConfigFileUpdates: true
  }
}

// The workflow reads the deployment token with this role, so no token is stored anywhere.
resource deployContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(staticWebApp.id, deployIdentityId, contributorRoleId)
  scope: staticWebApp
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', contributorRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
  }
}

output name string = staticWebApp.name
output hostname string = staticWebApp.properties.defaultHostname
