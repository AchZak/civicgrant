@description('Globally unique server name: lowercase letters, digits and hyphens, 3 to 63 characters.')
@minLength(3)
@maxLength(63)
param name string

@description('Azure region of the server.')
param location string

@description('Tags applied to the server.')
param tags object

@description('Major PostgreSQL version, the same one the application is tested against.')
param version string

@description('Entra principal that administers the server: objectId, principalName (the sign-in name) and principalType (User, Group or ServicePrincipal).')
param administrator object

@description('Names of the databases to create, one per environment.')
param databaseNames array

@description('Compute size. Standard_B1ms is the one covered by the free services.')
param skuName string = 'Standard_B1ms'

// The free services cover 32 GB. Billing counts what's provisioned, not what's used.
@description('Provisioned storage in GiB.')
param storageSizeGB int = 32

resource server 'Microsoft.DBforPostgreSQL/flexibleServers@2025-08-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: 'Burstable'
  }
  properties: {
    version: version

    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Disabled'
      tenantId: tenant().tenantId
    }

    storage: {
      storageSizeGB: storageSizeGB

      // Off: growing past 32 GiB would start billing without warning.
      autoGrow: 'Disabled'

      // Azure's defaults for 32 GiB, declared so what-if stays clean.
      type: 'Premium_LRS'
      tier: 'P4'
      iops: 120
    }

    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }

    dataEncryption: {
      type: 'SystemManaged'
    }

    replicationRole: 'Primary'
    replica: {
      role: 'Primary'
    }

    highAvailability: {
      mode: 'Disabled'
    }

    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

// The server takes one change at a time, so its child resources wait for each other.
resource entraAdministrator 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2025-08-01' = {
  parent: server
  name: administrator.objectId // Must be the principal's object id.
  properties: {
    principalName: administrator.principalName
    principalType: administrator.principalType
    tenantId: tenant().tenantId
  }
}

// 0.0.0.0 means any address inside Azure, other customers included. A token is still required.
resource allowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2025-08-01' = {
  parent: server
  name: 'AllowAllAzureServicesAndResourcesWithinAzureIps'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
  dependsOn: [
    entraAdministrator
  ]
}

@batchSize(1)
resource databases 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2025-08-01' = [
  for databaseName in databaseNames: {
    parent: server
    name: databaseName
    properties: {
      charset: 'UTF8'
      collation: 'en_US.utf8'
    }
    dependsOn: [
      allowAzureServices
    ]
  }
]

resource deleteLock 'Microsoft.Authorization/locks@2020-05-01' = {
  scope: server
  name: 'lock-${name}'
  properties: {
    level: 'CanNotDelete'
    notes: 'Shared by every environment and holds their data. Remove this lock in Bicep first if the server must really go.'
  }
}

output name string = server.name
output host string = server.properties.fullyQualifiedDomainName
