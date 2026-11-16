// ============================================================
// MODULE: DOCUMENTS STORAGE
// ============================================================
//
// Creates the storage account that holds the supporting documents (PDFs) of every
// environment:
//
//   stcivicgrant<suffix>          storage account, reachable only with Entra tokens
//     blob service                deleted blobs and containers are kept 7 days
//     documents-<env>             one private container per environment
//       role assignment           the environment's app identity may read and write it
//     delete lock                 nobody can delete the account by mistake
//
// Like the PostgreSQL server, it lives with the shared resources: deleting and
// recreating an environment never touches the documents. Each app identity reaches
// its own container and nothing else, the same way each one reaches only its own
// database.
//
// Cost: Hot LRS, about $0.02 per GB a month plus a few cents per 10,000 operations.
// A test environment's PDFs cost well under a cent a month.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Globally unique account name: 3 to 24 lowercase letters and digits.')
@minLength(3)
@maxLength(24)
param name string

@description('Azure region of the account: next to the database and the apps.')
param location string

@description('Tags applied to the account.')
param tags object

@description('One entry per container: name, and the resource id (writerIdentityId) and object id (writerPrincipalId) of the identity that reads and writes it.')
param containers array

@description('Days a deleted blob or container can still be restored.')
param deleteRetentionDays int = 7


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Id of a built-in Azure role, the same in every tenant.
// To look one up: az role definition list --name '<role name>' --query "[0].name" --output tsv
var blobDataContributorRoleId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe' // Storage Blob Data Contributor


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The storage account.
resource account 'Microsoft.Storage/storageAccounts@2026-09-01' = {
  name: name
  location: location
  tags: tags
  kind: 'StorageV2'           // The general-purpose kind Microsoft recommends for blobs.
  sku: {
    name: 'Standard_LRS'      // Three copies in one datacenter: the cheapest redundancy.
  }
  properties: {
    accessTier: 'Hot'         // Documents are read soon after upload; Cool would charge more per read.

    // No account keys and no shared access signatures signed with them: every request
    // needs a Microsoft Entra token. Nothing secret exists to store, leak or rotate.
    allowSharedKeyAccess: false
    defaultToOAuthAuthentication: true  // The portal and tools sign in with Entra too.

    // No container can ever be made public, whatever its own setting says.
    allowBlobPublicAccess: false

    // Encrypted connections only, with a current TLS version.
    supportsHttpsTrafficOnly: true
    minimumTlsVersion: 'TLS1_2'

    // Replication to accounts in other tenants is off: the documents stay in this one.
    allowCrossTenantReplication: false

    // Reachable over the internet, but only with a valid token for a role on a
    // container. Private networking is out of scope, as for the database.
    publicNetworkAccess: 'Enabled'
  }
}

// 2. The blob service: soft delete keeps a deleted blob or container restorable for
//    a few days, then removes it for good. That covers a mistake without keeping a
//    document an applicant removed for longer than a week.
resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2026-06-01' = {
  parent: account
  name: 'default'  // An account has exactly one blob service, always named default.
  properties: {
    deleteRetentionPolicy: {
      enabled: true
      days: deleteRetentionDays
    }
    containerDeleteRetentionPolicy: {
      enabled: true
      days: deleteRetentionDays
    }
  }
}

// 3. One private container per environment.
resource documentContainers 'Microsoft.Storage/storageAccounts/blobServices/containers@2026-06-01' = [
  for container in containers: {
    parent: blobService
    name: container.name
    properties: {
      publicAccess: 'None'
    }
  }
]

// 4. Each environment's app identity may read, write and delete blobs in its own
//    container: Storage Blob Data Contributor, scoped to that one container.
resource writerRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for (container, i) in containers: {
    // Seeded with the identity's resource id, known before deployment, so what-if
    // can show the assignment in full.
    name: guid(documentContainers[i].id, container.writerIdentityId, blobDataContributorRoleId)
    scope: documentContainers[i]
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', blobDataContributorRoleId)
      principalId: container.writerPrincipalId
      principalType: 'ServicePrincipal'
      description: 'Read and write the documents of one environment.'
    }
  }
]

// 5. A delete lock on the account, as on the PostgreSQL server. It only governs
//    Azure's management API: the app can still delete blobs inside its container.
resource deleteLock 'Microsoft.Authorization/locks@2020-05-01' = {
  scope: account
  name: 'lock-${name}'
  properties: {
    level: 'CanNotDelete'
    notes: 'Holds the documents of every environment. Remove this lock in Bicep first if the account must really go.'
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output name string = account.name
output blobEndpoint string = account.properties.primaryEndpoints.blob  // https://<name>.blob.core.windows.net/
