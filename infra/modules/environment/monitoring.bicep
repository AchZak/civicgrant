@description('Name of the Log Analytics workspace.')
param workspaceName string

@description('Name of the Application Insights resource.')
param applicationInsightsName string

@description('Azure region of both resources.')
param location string

@description('Tags applied to both resources.')
param tags object

@description('Resource id of the identity that sends telemetry. Seeds the role assignment name.')
param publisherIdentityId string

@description('Object id of the identity that sends telemetry.')
param publisherPrincipalId string

// A string because Bicep has no decimals. At the cap, ingestion stops until the next day.
@description('Daily ingestion cap in GB, written as a decimal string.')
param dailyCapGb string = '0.1'

var metricsPublisherRoleId = '3913510d-42f4-4e42-8a64-420c390055eb'

resource workspace 'Microsoft.OperationalInsights/workspaces@2026-03-01' = {
  name: workspaceName
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }

    retentionInDays: 30

    workspaceCapping: {
      dailyQuotaGb: json(dailyCapGb)
    }

    features: {
      disableLocalAuth: true

      // Azure's default, declared so what-if stays clean.
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: applicationInsightsName
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: workspace.id
    IngestionMode: 'LogAnalytics'
    DisableLocalAuth: true
  }
}

resource publisherRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(applicationInsights.id, publisherIdentityId, metricsPublisherRoleId)
  scope: applicationInsights
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', metricsPublisherRoleId)
    principalId: publisherPrincipalId
    principalType: 'ServicePrincipal'
  }
}

output workspaceId string = workspace.id
output applicationInsightsId string = applicationInsights.id
output connectionString string = applicationInsights.properties.ConnectionString
