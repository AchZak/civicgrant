// ============================================================
// MODULE: CONTAINER REGISTRY
// ============================================================
//
// Creates the registry that stores the application image, and gives the
// listed principals the right to push to it.
//
// One registry serves every environment: the image that passed acceptance
// is exactly the one that goes to production.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

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


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Ids of built-in Azure roles. They're the same in every tenant.
var repositoryWriterRoleId = '2a1e307c-b015-4ebd-883e-5b7698a07328' // Container Registry Repository Writer
var readerRoleId = 'acdd72a7-3385-48ef-bd42-f606fba81ae7'           // Reader


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The registry.
resource registry 'Microsoft.ContainerRegistry/registries@2025-11-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: sku
  }
  properties: {
    // The admin user is one shared username and password for the whole registry.
    // Off: only Entra identities with a role on the registry can pull or push.
    adminUserEnabled: false

    // ABAC mode: access comes from the Container Registry Repository roles,
    // which can be limited to single repositories.
    // AcrPull and AcrPush have no effect in this mode.
    roleAssignmentMode: 'AbacRepositoryPermissions'
  }
}

// 2. Push rights: Container Registry Repository Writer, one assignment per principal.
//    A role assignment links three things:
//      who:   principalId
//      what:  roleDefinitionId, the role
//      where: scope, here this registry only, not the resource group or subscription
resource pushRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in pushPrincipalIds: {
    // The name must be a GUID. guid() derives it from its inputs, so a redeploy produces
    // the same name and updates the assignment instead of adding a duplicate.
    name: guid(registry.id, principalId, repositoryWriterRoleId)
    scope: registry
    properties: {
      // Built-in role definitions are referenced at subscription level.
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryWriterRoleId)
      principalId: principalId

      // A managed identity is a service principal in Entra. Saying so skips a lookup
      // that can fail for an identity created seconds ago, before Entra has replicated it.
      principalType: 'ServicePrincipal'
    }
  }
]

// 3. Read rights on the registry resource: Reader, one assignment per principal.
//    Writer only covers the images themselves. az acr login also reads the
//    registry resource, which needs Reader.
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


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output name string = registry.name                          // For az acr login in the pipeline.
output loginServer string = registry.properties.loginServer // <name>.azurecr.io, the host in image names.
