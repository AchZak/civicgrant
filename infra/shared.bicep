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
//
// Deployed by hand, once, from the project root:
//
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
param publishBranches array = [
  'develop'
  'main'
]

// Premium is left out on purpose: it costs about $50 a month.
@description('Container registry SKU.')
@allowed([
  'Basic'
  'Standard'
])
param registrySku string


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

// 3. The container registry, with push rights for the identity from step 2.
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
