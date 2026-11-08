// ============================================================
// MODULE: MONITORING
// ============================================================
//
// Creates where an environment's logs and telemetry are stored, and lets the
// application send telemetry:
//
//   Log Analytics workspace   stores every log: the platform's and the app's
//   Application Insights      the app's view on top of it: requests, errors, traces
//     role assignment         the app identity may send telemetry
//
// Both accept Microsoft Entra sign-in only: no key is created anywhere.
//
// Sized to stay inside the free allowance: 5 GB of ingestion a month per billing
// account, shared by every environment, and 31 days of retention. The daily cap
// below is the safety net.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by main.bicep.
// ------------------------------------------------------------

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

// A string, because Bicep has no decimal numbers: json() turns it into one below.
// 0.1 GB a day is at most 3.1 GB a month. When the cap is reached, the workspace
// stops ingesting until the next day: data is lost, money isn't.
@description('Daily ingestion cap in GB, written as a decimal string.')
param dailyCapGb string = '0.1'


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Id of a built-in Azure role, the same in every tenant.
// To look one up: az role definition list --name '<role name>' --query "[0].name" --output tsv
var metricsPublisherRoleId = '3913510d-42f4-4e42-8a64-420c390055eb' // Monitoring Metrics Publisher


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The Log Analytics workspace.
resource workspace 'Microsoft.OperationalInsights/workspaces@2026-03-01' = {
  name: workspaceName
  location: location
  tags: tags
  properties: {
    // Pay-as-you-go, per GB ingested: the tier the 5 free GB a month apply to.
    sku: {
      name: 'PerGB2018'
    }

    // The minimum. The first 31 days of retention are free.
    retentionInDays: 30

    workspaceCapping: {
      dailyQuotaGb: json(dailyCapGb)
    }

    features: {
      // No shared key: only Entra identities with a role can send or read data.
      disableLocalAuth: true

      // Azure's default, declared so what-if compares like with like: reading a
      // resource's logs needs rights on that resource, not on the whole workspace.
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

// 2. Application Insights, storing its data in the workspace from step 1.
//    Workspace-based: it has no storage or billing of its own.
resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: applicationInsightsName
  location: location
  tags: tags
  kind: 'web'  // A web application or API. Only changes which views the portal shows first.
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: workspace.id
    IngestionMode: 'LogAnalytics'

    // Only Entra-authenticated telemetry is accepted. Without this, anyone holding
    // the connection string could send fake telemetry.
    DisableLocalAuth: true
  }
}

// 3. The identity may send telemetry: Monitoring Metrics Publisher, on this
//    Application Insights resource only.
resource publisherRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  // Seeded with the identity's resource id, not its object id: the id is known
  // before deployment, so what-if can show this assignment in full.
  name: guid(applicationInsights.id, publisherIdentityId, metricsPublisherRoleId)
  scope: applicationInsights
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', metricsPublisherRoleId)
    principalId: publisherPrincipalId
    principalType: 'ServicePrincipal'
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output workspaceId string = workspace.id                                       // Destination of the environment's logs.
output connectionString string = applicationInsights.properties.ConnectionString // Where the agent sends telemetry.
