targetScope = 'subscription'

@description('Azure regions where CivicGrant resources may be created.')
param allowedLocations array

@description('Tags every resource inherits from its resource group when it doesn\'t set them itself.')
param inheritedTagNames array

var denyRules = [
  {
    referenceId: 'allowedLocations'
    definitionId: 'e56962a6-4747-49cd-b67b-bf8b01975c4c'
    version: '1.*.*'
    parameters: {
      listOfAllowedLocations: { value: allowedLocations }
    }
  }
  {
    referenceId: 'storageNoSharedKey'
    definitionId: '8c6a50c6-9ffd-4ae7-986f-5fa6111f9a54'
    version: '2.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageNoPublicAccess'
    definitionId: '4fa4b6c0-31ca-4c0d-b10d-24b96f62a751'
    version: '3.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageHttpsOnly'
    definitionId: '404c3081-a854-4457-ae30-26a93ef643f9'
    version: '2.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageMinimumTls12'
    definitionId: 'fe83a0eb-a853-422d-aac2-1bffd182c5d0'
    version: '1.*.*'
    parameters: {
      minimumTlsVersion: { value: 'TLS1_2' }
    }
  }
  {
    referenceId: 'registryNoAdminUser'
    definitionId: 'dc921057-6b28-4fbe-9b83-f7bec05db6c2'
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'registryNoAnonymousPull'
    definitionId: '9f2dea28-e834-476c-99c5-3507b4728395'
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'containerAppHttpsOnly'
    definitionId: '0e80e269-43a4-4ae9-b5bc-178126b8a5cb'
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'containerAppManagedIdentity'
    definitionId: 'b874ab2d-72dd-47f1-8cb5-4a306478a4e7'
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'logAnalyticsEntraOnly'
    definitionId: 'e15effd4-2278-4c65-a0da-4d6f6d1890e2'
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'applicationInsightsEntraOnly'
    definitionId: '199d5677-e4d9-4264-9465-efe1839c06bd'
    version: '1.*.*'
    parameters: {}
  }
]

// For resources Azure creates on its own, like Application Insights' Smart Detection action group.
var inheritTagDefinitionId = 'ea3f2387-9b95-492a-a190-fcdc54f7b070'
var inheritTagVersion = '1.*.*'

resource guardrails 'Microsoft.Authorization/policySetDefinitions@2026-07-01' = {
  name: 'civicgrant-guardrails'
  properties: {
    displayName: 'CivicGrant guardrails'
    description: 'The rules every CivicGrant resource follows: EU regions, no keys or public access, HTTPS only, Entra-only telemetry, and the project tags.'
    policyType: 'Custom'
    version: '1.0.0'
    metadata: {
      category: 'CivicGrant'
    }
    policyDefinitions: concat(
      map(denyRules, rule => {
        policyDefinitionReferenceId: rule.referenceId
        policyDefinitionId: tenantResourceId('Microsoft.Authorization/policyDefinitions', rule.definitionId)
        definitionVersion: rule.version
        parameters: union(rule.parameters, { effect: { value: 'Deny' } })
      }),
      map(inheritedTagNames, tagName => {
        policyDefinitionReferenceId: 'inheritTag-${tagName}'
        policyDefinitionId: tenantResourceId('Microsoft.Authorization/policyDefinitions', inheritTagDefinitionId)
        definitionVersion: inheritTagVersion
        parameters: {
          tagName: { value: tagName }
        }
      })
    )
  }
}

output id string = guardrails.id
