@description('Name of the Container Apps environment.')
param name string

@description('Azure region of the environment.')
param location string

@description('Tags applied to the environment.')
param tags object

@description('Resource id of the Log Analytics workspace that receives the logs.')
param workspaceId string

resource environment 'Microsoft.App/managedEnvironments@2026-07-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    workloadProfiles: [
      {
        name: 'Consumption'
        workloadProfileType: 'Consumption'
      }
    ]

    // log-analytics would need the workspace's shared key, and local auth is off.
    appLogsConfiguration: {
      destination: 'azure-monitor'
    }

    zoneRedundant: false
    publicNetworkAccess: 'Enabled'
  }
}

// Preview on purpose: the only stable version allows one setting per resource, named 'service'.
#disable-next-line use-recent-api-versions
resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'logs-to-workspace'
  scope: environment
  properties: {
    workspaceId: workspaceId
    logs: [
      {
        category: 'ContainerAppConsoleLogs'
        enabled: true
      }
      {
        category: 'ContainerAppSystemLogs'
        enabled: true
      }
    ]
  }
}

output id string = environment.id
