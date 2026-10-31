// ============================================================
// MODULE: PIPELINE IDENTITY
// ============================================================
//
// Creates the identity GitHub Actions logs in to Azure with, without any
// stored password (workload identity federation).
//
// How the login works:
//   1. For each workflow run, GitHub signs a short-lived token that states
//      which repository and branch the run is for.
//   2. azure/login sends that token to Entra, with this identity's client id.
//   3. Entra checks the token against the federated credentials below. If one
//      matches exactly, it returns an Azure token for this identity.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Name of the managed identity used by GitHub Actions.')
param name string

@description('Azure region of the identity.')
param location string

@description('Tags applied to the identity.')
param tags object

@description('Start of the GitHub OIDC subject claim for the repository, without the trailing colon.')
param githubSubjectPrefix string

@description('Branches whose workflow runs may log in with this identity.')
param branches array


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The identity.
//    User-assigned: a standalone resource with its own lifecycle. A system-assigned
//    identity belongs to one resource and is deleted with it.
resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: name
  location: location
  tags: tags
}

// 2. One federated credential per branch.
//    @batchSize(1) creates them one at a time: Azure rejects concurrent changes
//    to the federated credentials of one identity.
@batchSize(1)
resource branchCredentials 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2024-11-30' = [
  for branch in branches: {
    parent: identity  // Child resource of the identity from step 1.
    name: 'github-branch-${branch}'
    properties: {
      // Who signs the tokens: GitHub Actions.
      issuer: 'https://token.actions.githubusercontent.com'

      // What the token must say, character for character. Wildcards aren't supported.
      // Pull request runs have a different subject, so they can't log in.
      subject: '${githubSubjectPrefix}:ref:refs/heads/${branch}'

      // Who the token is meant for. azure/login asks GitHub for this audience.
      audiences: [
        'api://AzureADTokenExchange'
      ]
    }
  }
]


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output principalId string = identity.properties.principalId // Object id in Entra: role assignments point to it.
output clientId string = identity.properties.clientId       // Sent by azure/login to say which identity logs in.
