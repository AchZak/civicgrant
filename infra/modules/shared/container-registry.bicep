// ============================================================
// MODULE: CONTAINER REGISTRY
// ============================================================
//
// Creates the registry that stores the application image, gives the listed
// principals the right to push to it, and lets the listed users look inside it.
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

@description('Entra user object ids allowed to list repositories and read images, without changing anything.')
param viewerUserIds array = []


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Ids of built-in Azure roles. They're the same in every tenant.
// To look one up: az role definition list --query "[?roleName=='<role name>'].name" --output tsv
var repositoryWriterRoleId = '2a1e307c-b015-4ebd-883e-5b7698a07328' // Container Registry Repository Writer
var repositoryReaderRoleId = 'b93aa761-3e63-49ed-ac28-beffa264f7ac' // Container Registry Repository Reader
var catalogListerRoleId = 'bfdb9389-c9a5-478a-bb2f-ba9ca092c3c7'    // Container Registry Repository Catalog Lister
var readerRoleId = 'acdd72a7-3385-48ef-bd42-f606fba81ae7'           // Reader (the general one, for the registry resource)


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

    // The next three are the values Azure uses when they're left out. They're
    // written down so what-if compares like with like and stays clean.

    // Dedicated data endpoints (a Premium feature, for firewall rules). Off: image
    // layers are served from the registry's shared endpoint.
    dataEndpointEnabled: false

    // Customer-managed key encryption (Premium). Disabled doesn't mean unencrypted:
    // images are always encrypted at rest, here with keys Microsoft manages.
    encryption: {
      status: 'disabled'
    }

    // Whether ACR Tasks may bypass network rules. No tasks and no network rules
    // here, so the secure default.
    networkRuleBypassAllowedForTasks: false
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

// 4. Image read rights for viewers: Container Registry Repository Reader.
//    In ABAC mode, Owner and Contributor manage the registry resource but can't see
//    inside it. This role shows tags and manifests and allows pulling. No push, no delete.
resource viewerReadRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for userId in viewerUserIds: {
    name: guid(registry.id, userId, repositoryReaderRoleId)
    scope: registry
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', repositoryReaderRoleId)
      principalId: userId
      principalType: 'User'  // A person, not an identity: the type must match.
    }
  }
]

// 5. Repository list rights for viewers: Container Registry Repository Catalog Lister.
//    The Repository roles let you read a repository you already know the name of,
//    but not list which repositories exist. The portal's Repositories page needs this one.
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


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output name string = registry.name                          // For az acr login in the pipeline.
output loginServer string = registry.properties.loginServer // <name>.azurecr.io, the host in image names.
