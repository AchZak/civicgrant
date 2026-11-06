// ============================================================
// MODULE: POSTGRESQL SERVER
// ============================================================
//
// Creates the PostgreSQL server every environment shares, one database per
// environment on it, and a lock that stops anyone deleting the server by mistake.
//
// Sign-in is Microsoft Entra only: no password exists anywhere. The person deploying
// becomes the server's Entra administrator; applications get their own database
// roles later, from infra/bootstrap (Bicep can't create database roles).
//
// Sized to stay inside the free services: Burstable B1ms with 32 GiB of storage.
// Changing the size, the storage or the backup settings below can start costing money.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

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

// The free services cover 750 hours a month of exactly this size: one server running all month.
@description('Compute size. Standard_B1ms is the one covered by the free services.')
param skuName string = 'Standard_B1ms'

// The free services cover 32 GB of provisioned storage. Billing counts what's
// provisioned, not what's used, so this is the ceiling, not a starting point.
@description('Provisioned storage in GiB.')
param storageSizeGB int = 32


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The server.
//    2025-08-01, not the newer 2026-10-01: Bicep 0.48.1 has no types for 2026-10-01
//    yet, so it couldn't check the property names below. The linter's
//    use-recent-api-versions rule warns once a Bicep release knows the newer one.
resource server 'Microsoft.DBforPostgreSQL/flexibleServers@2025-08-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: 'Burstable'  // Burstable earns CPU credits while idle and spends them under load: fine for a test workload.
  }
  properties: {
    version: version

    // Entra sign-in only. With password authentication off, no administrator
    // password is ever created, so there is none to store, leak or rotate.
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Disabled'
      tenantId: tenant().tenantId  // The Entra tenant whose users and identities may sign in.
    }

    storage: {
      storageSizeGB: storageSizeGB

      // Off on purpose: auto-grow would raise the provisioned size past 32 GiB when
      // the disk fills up, and billing would start without warning. If the disk ever
      // fills, the server turns read-only instead: visible, and free.
      autoGrow: 'Disabled'
    }

    backup: {
      // Automatic daily backups, kept 7 days: a point-in-time restore can rewind the
      // data to any moment in that window. 7 is the minimum, and stays inside the
      // 32 GB of free backup storage.
      backupRetentionDays: 7

      // Copies to a paired region cost extra, and Belgium Central has no paired region.
      geoRedundantBackup: 'Disabled'
    }

    // A standby server would double the compute cost. One server is enough for test.
    highAvailability: {
      mode: 'Disabled'
    }

    // Reachable over the internet, but only from the addresses the firewall rules
    // below allow, and only with a valid Entra token. Private networking is out of scope.
    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

// 2. The Entra administrator: the person deploying this file.
//    It can create database roles and grant rights, which is what infra/bootstrap needs.
//    Child resources of one server are created one after the other (dependsOn below):
//    the server accepts only one change at a time and rejects concurrent ones.
resource entraAdministrator 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2025-08-01' = {
  parent: server
  name: administrator.objectId  // The resource name must be the principal's object id.
  properties: {
    principalName: administrator.principalName  // The name you sign in to PostgreSQL with.
    principalType: administrator.principalType
    tenantId: tenant().tenantId
  }
}

// 3. Firewall: let in connections coming from Azure services, such as Container Apps.
//    0.0.0.0 to 0.0.0.0 is Azure's special value for "any address inside Azure",
//    including other customers' resources. A connection still needs a valid Entra
//    token for a role on this server, so the firewall is only the first gate.
resource allowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2025-08-01' = {
  parent: server
  name: 'AllowAllAzureServicesAndResourcesWithinAzureIps'  // The name the portal uses for this rule.
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
  dependsOn: [
    entraAdministrator
  ]
}

// 4. One database per environment.
//    @batchSize(1) creates them one at a time, for the same reason as above.
@batchSize(1)
resource databases 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2025-08-01' = [
  for databaseName in databaseNames: {
    parent: server
    name: databaseName
    properties: {
      charset: 'UTF8'           // Stores any language: Dutch and French accents included.
      collation: 'en_US.utf8'   // How text sorts and compares. Matches the local database.
    }
    dependsOn: [
      allowAzureServices
    ]
  }
]

// 5. A delete lock on the server.
//    Nobody can delete the server, not even an Owner, until someone removes the lock
//    on purpose first. It also covers the server's child resources: databases and
//    firewall rules can't be deleted through Azure while it exists. SQL inside the
//    databases (DROP TABLE, Flyway migrations) isn't affected: locks only govern
//    Azure's management API.
resource deleteLock 'Microsoft.Authorization/locks@2020-05-01' = {
  scope: server
  name: 'lock-${name}'
  properties: {
    level: 'CanNotDelete'
    notes: 'Shared by every environment and holds their data. Remove this lock in Bicep first if the server must really go.'
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output name string = server.name                                 // For az postgres commands.
output host string = server.properties.fullyQualifiedDomainName  // <name>.postgres.database.azure.com, the host in JDBC URLs.
