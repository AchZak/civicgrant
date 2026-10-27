@description('Name of the managed identity used by GitHub Actions.')
param name string

param location string
param tags object

@description('GitHub repository trusted to log in, as owner/name.')
param githubRepository string

@description('Branches whose workflow runs may log in with this identity.')
param branches array

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: name
  location: location
  tags: tags
}

@batchSize(1)
resource branchCredentials 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = [
  for branch in branches: {
    parent: identity
    name: 'github-branch-${branch}'
    properties: {
      issuer: 'https://token.actions.githubusercontent.com'
      subject: 'repo:${githubRepository}:ref:refs/heads/${branch}'
      audiences: [
        'api://AzureADTokenExchange'
      ]
    }
  }
]

output principalId string = identity.properties.principalId
output clientId string = identity.properties.clientId
