// ============================================================
// SHARED INFRASTRUCTURE
// ============================================================
//
// Creates the resources every environment shares:
//
//   rg-civicgrant-shared
//     id-civicgrant-pipeline     identity GitHub Actions logs in to Azure with
//       github-branch-develop    federated credential: runs on develop may log in
//       github-branch-main       federated credential: runs on main may log in
//     crcivicgrant<suffix>       container registry for the application image
//       2 role assignments       the pipeline identity may push images
//       2 role assignments       the person deploying may list and read images
//     psql-civicgrant-<suffix>   PostgreSQL server shared by every environment, Entra sign-in only,
//                                in its own region (postgresLocation)
//       administrator            the person deploying administers it
//       firewall rule            connections from Azure services are let in
//       civicgrant_<env>         one database per environment
//       delete lock              nobody can delete the server by mistake
//
//   subscription
//     budget-civicgrant-monthly  emails when spending approaches or passes €5 a month
//
// Deployed by hand, from the project root. BUDGET_ALERT_EMAIL must be set, because
// shared.bicepparam reads the alert address from it:
//
//   export BUDGET_ALERT_EMAIL=you@example.com
//   az bicep lint --file infra/shared.bicep
//   az deployment sub what-if --location belgiumcentral --name civicgrant-shared --parameters infra/shared.bicepparam
//   az deployment sub create  --location belgiumcentral --name civicgrant-shared --parameters infra/shared.bicepparam
//
// The pipeline can't deploy this file: it creates the identity the pipeline logs in with.
// Running it again is safe. It updates what already exists instead of duplicating it.


// ------------------------------------------------------------
// SCOPE
// ------------------------------------------------------------

// A deployment normally targets a resource group. This one targets the subscription,
// because it creates the resource group itself.
targetScope = 'subscription'


// ------------------------------------------------------------
// PARAMETERS
// Values come from shared.bicepparam.
// ------------------------------------------------------------

@description('Azure region for the shared resources.')
param location string

@description('GitHub OIDC subject prefix of the repository, as shown under Settings > Actions > OIDC: repo:owner/name, or repo:owner@id/name@id for repositories created or renamed since July 2026.')
param githubSubjectPrefix string

@description('Branches whose workflow runs may push images.')
param publishBranches array

// Premium is left out on purpose: it costs about $50 a month.
@description('Container registry SKU.')
@allowed([
  'Basic'
  'Standard'
])
param registrySku string

@description('Email address that receives the budget alerts.')
param budgetAlertEmail string

@description('Major PostgreSQL version of the shared server.')
param postgresVersion string

// Separate from location: the server may have to live in another region than the
// other shared resources, when location has no capacity for its size.
@description('Azure region of the PostgreSQL server. The environments\' apps run in the same region, so their queries never cross regions.')
param postgresLocation string

@description('Databases on the shared PostgreSQL server, one per environment.')
param postgresDatabaseNames array


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Applied to every resource. The portal and the cost reports can filter on them.
var tags = {
  project: 'civicgrant'    // Which project the resource belongs to.
  environment: 'shared'    // shared, tst, acc or prd.
  managedBy: 'bicep'       // Change it through Bicep, not in the portal.
}


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The resource group that holds everything below.
resource resourceGroup 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: 'rg-civicgrant-shared'
  location: location
  tags: tags
}

// 2. The identity GitHub Actions logs in with, and its federated credentials.
//    A module is another Bicep file, deployed as its own nested deployment.
//    "scope: resourceGroup" deploys it into the group from step 1,
//    so Azure waits until the group exists.
module pipelineIdentity 'modules/pipeline-identity.bicep' = {
  scope: resourceGroup
  params: {
    name: 'id-civicgrant-pipeline'
    location: location
    tags: tags
    githubSubjectPrefix: githubSubjectPrefix
    branches: publishBranches
  }
}

// 3. The container registry, with push rights for the identity from step 2
//    and read rights for the person deploying this file.
module registry 'modules/container-registry.bicep' = {
  scope: resourceGroup
  params: {
    // Registry names are global DNS names (<name>.azurecr.io), so they need a unique suffix.
    // uniqueString() hashes the subscription id into 13 characters. The same subscription
    // always gives the same suffix, so a redeploy finds the existing registry.
    name: 'crcivicgrant${uniqueString(subscription().subscriptionId)}'
    location: location
    tags: tags
    sku: registrySku

    // Using an output of step 2 makes Azure finish step 2 first.
    // No explicit dependsOn is needed.
    pushPrincipalIds: [
      pipelineIdentity.outputs.principalId
    ]

    // deployer() is the identity running this deployment: you, from your machine.
    // Its object id comes from your az login, so no personal id is stored in this
    // public repository.
    viewerUserIds: [
      deployer().objectId
    ]
  }
}

// 4. The monthly budget. It watches the whole subscription, so every environment
//    deployed later is covered too. No scope line: a module defaults to the scope
//    of the file calling it, here the subscription.
module budget 'modules/budget.bicep' = {
  params: {
    name: 'budget-civicgrant-monthly'
    amount: 5
    startDate: '2026-11-01T00:00:00Z' // Created in November 2026. Never change it.
    endDate: '2036-11-01T00:00:00Z'
    contactEmails: [
      budgetAlertEmail
    ]
  }
}

// 5. The PostgreSQL server, its databases and its delete lock.
//    It lives here, outside every environment, so deleting or recreating an
//    environment can never touch the server or the data on it.
module postgres 'modules/postgresql-server.bicep' = {
  scope: resourceGroup
  params: {
    // Server names are global DNS names (<name>.postgres.database.azure.com),
    // so they get a unique suffix like the registry's. The region is part of the hash:
    // a server in another region is a different server, so it gets a different name.
    // A failed create in Belgium Central left its name reserved there.
    name: 'psql-civicgrant-${uniqueString(subscription().subscriptionId, postgresLocation)}'
    location: postgresLocation
    tags: tags
    version: postgresVersion
    databaseNames: postgresDatabaseNames

    // You, from your az login, as with the registry viewer roles: no personal
    // value is stored in this public repository.
    administrator: {
      objectId: deployer().objectId
      principalName: deployer().userPrincipalName
      principalType: 'User'
    }
  }
}


// ------------------------------------------------------------
// OUTPUTS
// Read after deployment with az deployment sub show, and stored as
// GitHub repository variables. None of them is a secret.
// ------------------------------------------------------------

output registryName string = registry.outputs.name                  // ACR_NAME
output registryLoginServer string = registry.outputs.loginServer    // <name>.azurecr.io
output pipelineClientId string = pipelineIdentity.outputs.clientId  // AZURE_CLIENT_ID
output tenantId string = tenant().tenantId                          // AZURE_TENANT_ID
output subscriptionId string = subscription().subscriptionId        // AZURE_SUBSCRIPTION_ID
output postgresServerName string = postgres.outputs.name            // For az postgres and the bootstrap runbook.
output postgresHost string = postgres.outputs.host                  // Host of the JDBC URL.
