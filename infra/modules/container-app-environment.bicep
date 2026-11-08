// ============================================================
// MODULE: CONTAINER APPS ENVIRONMENT
// ============================================================
//
// Creates the environment an environment's container apps run in, and sends its
// logs to the Log Analytics workspace:
//
//   Container Apps environment   the shared host: network, ingress, scaling
//     diagnostic setting         console and system logs go to the workspace
//
// A Container Apps environment is to container apps what an application server
// was to WAR files: they run inside it and share its network and its logs.
//
// Free: a workload profiles environment using only its built-in Consumption
// profile has no fixed fee. Only the apps' own usage is billed.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by main.bicep.
// ------------------------------------------------------------

@description('Name of the Container Apps environment.')
param name string

@description('Azure region of the environment.')
param location string

@description('Tags applied to the environment.')
param tags object

@description('Resource id of the Log Analytics workspace that receives the logs.')
param workspaceId string


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The environment.
resource environment 'Microsoft.App/managedEnvironments@2026-07-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    // A workload profiles environment, the type Microsoft recommends for new ones,
    // with only the built-in Consumption profile: serverless, billed per second of
    // use, able to scale to zero. Dedicated profiles would add a fixed fee.
    workloadProfiles: [
      {
        name: 'Consumption'  // The built-in profile's fixed name. Apps refer to it.
        workloadProfileType: 'Consumption'
      }
    ]

    // Logs go through Azure Monitor, routed by the diagnostic setting in step 2.
    // The other destination, log-analytics, needs the workspace's shared key,
    // which the workspace doesn't have: its local authentication is off.
    appLogsConfiguration: {
      destination: 'azure-monitor'
    }

    // Spreading replicas across availability zones needs a virtual network of
    // our own, out of scope here.
    zoneRedundant: false

    // Reachable from the internet. The app's ingress decides what's exposed: HTTPS only.
    publicNetworkAccess: 'Enabled'
  }
}

// 2. Send the environment's logs to the workspace.
//    A diagnostic setting attaches to another resource (scope) and says which of
//    its log categories go where.
//    A preview version, on purpose: the only stable one, 2016-09-01, is the legacy
//    API that allows a single setting per resource, which must be named 'service'.
//    Named settings exist only from 2017-05-01-preview, and Azure Monitor has never
//    released them as stable. The linter suggests 2016-09-01 because it only
//    compares dates, so its warning is switched off for this line alone.
#disable-next-line use-recent-api-versions
resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'logs-to-workspace'
  scope: environment
  properties: {
    workspaceId: workspaceId
    logs: [
      {
        category: 'ContainerAppConsoleLogs'  // What the app writes to stdout and stderr.
        enabled: true
      }
      {
        category: 'ContainerAppSystemLogs'   // The platform's events: image pulls, probes, restarts, scaling.
        enabled: true
      }
    ]
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output id string = environment.id  // The container apps' environmentId.
