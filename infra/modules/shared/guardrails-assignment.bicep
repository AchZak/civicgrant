@description('Resource id of the guardrails initiative.')
param policySetDefinitionId string

@description('Region where the assignment\'s identity is created. Any region works; the group\'s own is used.')
param location string

@description('Default enforces the rules; DoNotEnforce only reports on them.')
@allowed([
  'Default'
  'DoNotEnforce'
])
param enforcementMode string

// Not Contributor, which the inherit-tag definition names: changing tags is all it needs.
var tagContributorRoleId = '4a9ae827-6dc8-4573-8ac7-8239d42aa03f'

resource assignment 'Microsoft.Authorization/policyAssignments@2026-07-01' = {
  name: 'civicgrant-guardrails'
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    displayName: 'CivicGrant guardrails'
    policyDefinitionId: policySetDefinitionId
    definitionVersion: '1.*.*'
    enforcementMode: enforcementMode
    // Points to the README's Decisions section: keep that name.
    nonComplianceMessages: [
      {
        message: 'Refused by the CivicGrant guardrails (Azure Policy). See README.md, Decisions.'
      }
    ]
  }
}

resource tagContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, assignment.id, tagContributorRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', tagContributorRoleId)
    principalId: assignment.identity.principalId
    principalType: 'ServicePrincipal'
    description: 'Lets the CivicGrant guardrails add missing tags.'
  }
}
