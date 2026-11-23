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

var blobDataContributorRoleId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'

resource account 'Microsoft.Storage/storageAccounts@2026-06-01' = {
  name: name
  location: location
  tags: tags
  kind: 'StorageV2'
  sku: {
    name: 'Standard_LRS'
  }
  properties: {
    accessTier: 'Hot'
    allowSharedKeyAccess: false
    defaultToOAuthAuthentication: true
    allowBlobPublicAccess: false
    supportsHttpsTrafficOnly: true
    minimumTlsVersion: 'TLS1_2'
    allowCrossTenantReplication: false
    publicNetworkAccess: 'Enabled'
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2026-09-01' = {
  parent: account
  name: 'default'
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

resource documentContainers 'Microsoft.Storage/storageAccounts/blobServices/containers@2026-06-01' = [
  for container in containers: {
    parent: blobService
    name: container.name
    properties: {
      publicAccess: 'None'
    }
  }
]

resource writerRoleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for (container, i) in containers: {
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

resource deleteLock 'Microsoft.Authorization/locks@2020-05-01' = {
  scope: account
  name: 'lock-${name}'
  properties: {
    level: 'CanNotDelete'
    notes: 'Holds the documents of every environment. Remove this lock in Bicep first if the account must really go.'
  }
}

output name string = account.name
output blobEndpoint string = account.properties.primaryEndpoints.blob
