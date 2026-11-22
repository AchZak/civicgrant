targetScope = 'subscription'

@description('Azure region for the shared resources.')
param location string

@description('GitHub OIDC subject prefix of the repository, as shown under Settings > Actions > OIDC: repo:owner/name, or repo:owner@id/name@id for repositories created or renamed since July 2026.')
param githubSubjectPrefix string

@description('Branches whose workflow runs may push images.')
param publishBranches array

@description('Container registry SKU.')
@allowed([
  'Basic'
  'Standard'
])
param registrySku string

@description('Email address that receives the budget alerts and the monitoring alerts.')
param budgetAlertEmail string

@description('Major PostgreSQL version of the shared server.')
param postgresVersion string

@description('Azure region of the PostgreSQL server. The environments\' apps run in the same region, so their queries never cross regions.')
param postgresLocation string

@description('Environments to prepare: each gets a database, two identities and a resource group.')
param environmentNames array

@description('Default enforces the guardrails; DoNotEnforce only reports on them, to review a new rule before switching it on.')
@allowed([
  'Default'
  'DoNotEnforce'
])
param guardrailsEnforcementMode string

var tags = {
  project: 'civicgrant'
  environment: 'shared'
  managedBy: 'bicep'
}

var imageRepository = 'civicgrant'

var appIdentityIds = [
  for environmentName in environmentNames: resourceId(subscription().subscriptionId, resourceGroup.name, 'Microsoft.ManagedIdentity/userAssignedIdentities', 'id-civicgrant-app-${environmentName}')
]

resource resourceGroup 'Microsoft.Resources/resourceGroups@2025-04-01' = {
  name: 'rg-civicgrant-shared'
  location: location
  tags: tags
}

module pipelineIdentity 'modules/shared/pipeline-identity.bicep' = {
  scope: resourceGroup
  params: {
    name: 'id-civicgrant-pipeline'
    location: location
    tags: tags
    githubSubjectPrefix: githubSubjectPrefix
    branches: publishBranches
  }
}

module registry 'modules/shared/container-registry.bicep' = {
  scope: resourceGroup
  params: {
    name: 'crcivicgrant${uniqueString(subscription().subscriptionId)}'
    location: location
    tags: tags
    sku: registrySku

    pushPrincipalIds: [
      pipelineIdentity.outputs.principalId
    ]

    viewerUserIds: [
      deployer().objectId
    ]
  }
}

module budget 'modules/shared/budget.bicep' = {
  params: {
    name: 'budget-civicgrant-monthly'
    amount: 5
    startDate: '2026-11-01T00:00:00Z' // Never change it: a budget's start date is fixed once it exists.
    endDate: '2036-11-01T00:00:00Z'
    contactEmails: [
      budgetAlertEmail
    ]
  }
}

module postgres 'modules/shared/postgresql-server.bicep' = {
  scope: resourceGroup
  params: {
    // The region is part of the hash: a failed create left the Belgium Central name reserved.
    name: 'psql-civicgrant-${uniqueString(subscription().subscriptionId, postgresLocation)}'
    location: postgresLocation
    tags: tags
    version: postgresVersion
    databaseNames: [for environmentName in environmentNames: 'civicgrant_${environmentName}']

    administrator: {
      objectId: deployer().objectId
      principalName: deployer().userPrincipalName
      principalType: 'User'
    }
  }
}

module guardrails 'modules/shared/guardrails-definition.bicep' = {
  params: {
    allowedLocations: union([location], [postgresLocation])
    inheritedTagNames: objectKeys(tags)
  }
}

module sharedGuardrails 'modules/shared/guardrails-assignment.bicep' = {
  scope: resourceGroup
  params: {
    policySetDefinitionId: guardrails.outputs.id
    location: location
    enforcementMode: guardrailsEnforcementMode
  }
}

module environments 'modules/shared/environment-foundations.bicep' = [
  for environmentName in environmentNames: {
    params: {
      environmentName: environmentName
      location: postgresLocation
      tags: union(tags, { environment: environmentName })
      sharedResourceGroupName: resourceGroup.name
      githubSubjectPrefix: githubSubjectPrefix
      registryName: registry.outputs.name
      imageRepository: imageRepository
      guardrailsId: guardrails.outputs.id
      guardrailsEnforcementMode: guardrailsEnforcementMode
      alertActionGroupName: alertActionGroup.outputs.name
    }
  }
]

module documentsStorage 'modules/shared/documents-storage.bicep' = {
  scope: resourceGroup
  params: {
    name: 'stcivicgrant${take(uniqueString(subscription().subscriptionId, postgresLocation), 12)}'
    location: postgresLocation
    tags: tags
    containers: [
      for (environmentName, i) in environmentNames: {
        name: 'documents-${environmentName}'
        writerIdentityId: appIdentityIds[i]
        writerPrincipalId: environments[i].outputs.appPrincipalId
      }
    ]
  }
}

module alertActionGroup 'modules/shared/action-group.bicep' = {
  scope: resourceGroup
  params: {
    name: 'ag-civicgrant-shared'
    tags: tags
    shortName: 'civicgrant'
    email: budgetAlertEmail
  }
}

output registryName string = registry.outputs.name
output registryLoginServer string = registry.outputs.loginServer
output pipelineClientId string = pipelineIdentity.outputs.clientId
output tenantId string = tenant().tenantId
output subscriptionId string = subscription().subscriptionId
output postgresServerName string = postgres.outputs.name
output postgresHost string = postgres.outputs.host
output documentsEndpoint string = documentsStorage.outputs.blobEndpoint
output alertActionGroupId string = alertActionGroup.outputs.id

output environments array = [
  for (environmentName, i) in environmentNames: {
    name: environmentName
    resourceGroupName: environments[i].outputs.resourceGroupName
    deployClientId: environments[i].outputs.deployClientId
    appClientId: environments[i].outputs.appClientId
    appPrincipalId: environments[i].outputs.appPrincipalId
    appIdentityId: environments[i].outputs.appIdentityId
  }
]
