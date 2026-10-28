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

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2024-11-30' = {
  name: name
  location: location
  tags: tags
}

@batchSize(1)
resource branchCredentials 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2024-11-30' = [
  for branch in branches: {
    parent: identity
    name: 'github-branch-${branch}'
    properties: {
      issuer: 'https://token.actions.githubusercontent.com'
      subject: '${githubSubjectPrefix}:ref:refs/heads/${branch}'
      audiences: [
        'api://AzureADTokenExchange'
      ]
    }
  }
]

output principalId string = identity.properties.principalId
output clientId string = identity.properties.clientId
