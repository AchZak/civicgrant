// ============================================================
// MODULE: GUARDRAILS ASSIGNMENT
// ============================================================
//
// Applies the guardrails initiative to one resource group:
//
//   civicgrant-guardrails      policy assignment on the resource group
//     system identity          the identity Azure Policy acts as when it adds tags
//   Tag Contributor            role assignment: that identity may change tags in
//                              this group, and nothing else
//
// Deployed into each CivicGrant resource group: rg-civicgrant-shared and every
// rg-civicgrant-<env>. Only an Owner deploying shared.bicep by hand can change it:
// the environments' deploy identities are Contributors, which can't write or
// delete policy or role assignments.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep and environment.bicep.
// ------------------------------------------------------------

@description('Resource id of the guardrails initiative.')
param policySetDefinitionId string

@description('Region where the assignment\'s identity is created. Any region works; the group\'s own is used.')
param location string

// DoNotEnforce evaluates compliance without refusing or changing anything: used once,
// before switching a new rule on, to see what it would refuse.
@description('Default enforces the rules; DoNotEnforce only reports on them.')
@allowed([
  'Default'
  'DoNotEnforce'
])
param enforcementMode string


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Id of the built-in Tag Contributor role, the same in every tenant. It may read
// resources and write their tags (Microsoft.Resources/tags/*), but change nothing else.
// The inherit-tag definition names Contributor as its role, which the portal would
// grant automatically. Azure Policy doesn't require it: the identity only needs the
// rights for what it changes, and a remediation proves they're enough.
var tagContributorRoleId = '4a9ae827-6dc8-4573-8ac7-8239d42aa03f'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The assignment. Every create or update in this resource group is now checked
//    against the initiative, by whoever makes it: the portal, the CLI or a pipeline.
resource assignment 'Microsoft.Authorization/policyAssignments@2026-07-01' = {
  name: 'civicgrant-guardrails'
  // An identity needs a location; it's fixed once the assignment exists.
  location: location
  // A system-assigned identity lives and dies with the assignment: nobody can reuse
  // it elsewhere, and deleting the assignment removes it.
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    displayName: 'CivicGrant guardrails'
    policyDefinitionId: policySetDefinitionId
    // Any 1.x of the initiative: a new major is a change to review first.
    definitionVersion: '1.*.*'
    enforcementMode: enforcementMode
    // Shown in the error of every refused request, so whoever hits a rule knows
    // where it comes from and where it's explained.
    nonComplianceMessages: [
      {
        message: 'Refused by the CivicGrant guardrails (Azure Policy). See README.md, Decisions.'
      }
    ]
  }
}

// 2. The identity's right to change tags, here and nowhere else. Without it, the
//    tag rules still report missing tags, but can't add them.
resource tagContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  // Seeded with the assignment's id, which is known before deployment, so what-if
  // can show the role assignment in full.
  name: guid(resourceGroup().id, assignment.id, tagContributorRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', tagContributorRoleId)
    principalId: assignment.identity.principalId
    principalType: 'ServicePrincipal'
    description: 'Lets the CivicGrant guardrails add missing tags.'
  }
}
