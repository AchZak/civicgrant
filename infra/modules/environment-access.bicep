// ============================================================
// MODULE: ENVIRONMENT ACCESS
// ============================================================
//
// Gives an environment's deploy identity its rights on the environment's resource
// group, and nowhere else:
//
//   Contributor                              create and update the resources
//   Azure Deployment Stack Owner             manage the stack and its deny settings
//   Role Based Access Control Administrator  assign one role, under a condition
//
// Deployed into the environment's resource group (rg-civicgrant-<env>).


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by environment.bicep.
// ------------------------------------------------------------

@description('Resource id of the environment\'s deploy identity. Seeds the role assignment names.')
param deployIdentityId string

@description('Object id of the environment\'s deploy identity.')
param deployPrincipalId string


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Ids of built-in Azure roles, the same in every tenant.
// To look one up: az role definition list --name '<role name>' --query "[0].name" --output tsv
var contributorRoleId = 'b24988ac-6180-42a0-ab88-20f7382dd24c'          // Contributor
var stackOwnerRoleId = 'adb29209-aa1d-457b-a786-c913953d2891'           // Azure Deployment Stack Owner
var roleAdministratorRoleId = 'f58310d9-a9f6-439a-9e8d-f62e7b41a168'    // Role Based Access Control Administrator
var metricsPublisherRoleId = '3913510d-42f4-4e42-8a64-420c390055eb'     // Monitoring Metrics Publisher

// The one role the deploy identity may hand out: the environment template lets the
// app identity send telemetry to Application Insights with it.
// How to read it, in two halves:
//   - creating an assignment (write): only for Monitoring Metrics Publisher, and only
//     to a service principal, such as a managed identity, never to a user or a group;
//   - removing an assignment (delete): the same limits, checked on the existing one.
// Each half has its own attribute source: @Request is the assignment being created,
// @Resource the one being removed. Microsoft's "constrain roles and principal types"
// template, with ServicePrincipal as the only type.
var roleAdministratorCondition = '((!(ActionMatches{\'Microsoft.Authorization/roleAssignments/write\'})) OR (@Request[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${metricsPublisherRoleId}} AND @Request[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {\'ServicePrincipal\'})) AND ((!(ActionMatches{\'Microsoft.Authorization/roleAssignments/delete\'})) OR (@Resource[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${metricsPublisherRoleId}} AND @Resource[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {\'ServicePrincipal\'}))'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. Contributor: create, change and delete any resource in this group.
//    It can't assign roles, and it can't manage a stack's deny settings: Microsoft
//    removed that action from Contributor on purpose.
resource contributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  // The name is seeded with the identity's resource id, not its object id: the id is
  // known before deployment, so what-if can show these assignments in full.
  name: guid(resourceGroup().id, deployIdentityId, contributorRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', contributorRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// 2. Azure Deployment Stack Owner: create and update the environment's stack,
//    including its deny settings (denyDelete on tst). The Contributor variant of
//    this role can't create or delete deny assignments.
resource stackOwner 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, deployIdentityId, stackOwnerRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', stackOwnerRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// 3. Role Based Access Control Administrator, narrowed by the condition above.
//    Without the condition, this role could grant any role to anyone in the group,
//    Owner included. With it, the only possible grant is telemetry rights for an identity.
resource roleAdministrator 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, deployIdentityId, roleAdministratorRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleAdministratorRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
    condition: roleAdministratorCondition
    conditionVersion: '2.0'
    description: 'May only assign Monitoring Metrics Publisher, to service principals.'
  }
}
