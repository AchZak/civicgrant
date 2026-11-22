@description('Resource id of the environment\'s deploy identity. Seeds the role assignment names.')
param deployIdentityId string

@description('Object id of the environment\'s deploy identity.')
param deployPrincipalId string

var contributorRoleId = 'b24988ac-6180-42a0-ab88-20f7382dd24c'
var stackOwnerRoleId = 'adb29209-aa1d-457b-a786-c913953d2891'
var roleAdministratorRoleId = 'f58310d9-a9f6-439a-9e8d-f62e7b41a168'
var metricsPublisherRoleId = '3913510d-42f4-4e42-8a64-420c390055eb'

// The deploy identity may grant Monitoring Metrics Publisher to service principals, nothing else.
var roleAdministratorCondition = '((!(ActionMatches{\'Microsoft.Authorization/roleAssignments/write\'})) OR (@Request[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${metricsPublisherRoleId}} AND @Request[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {\'ServicePrincipal\'})) AND ((!(ActionMatches{\'Microsoft.Authorization/roleAssignments/delete\'})) OR (@Resource[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${metricsPublisherRoleId}} AND @Resource[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {\'ServicePrincipal\'}))'

resource contributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, deployIdentityId, contributorRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', contributorRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Not the Contributor variant: it can't manage the stack's deny settings.
resource stackOwner 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, deployIdentityId, stackOwnerRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', stackOwnerRoleId)
    principalId: deployPrincipalId
    principalType: 'ServicePrincipal'
  }
}

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
