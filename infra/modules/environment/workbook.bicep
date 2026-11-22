// ============================================================
// MODULE: WORKBOOK
// ============================================================
//
// Creates the environment's Azure Monitor Workbook: a report page of charts and
// tables, each filled by a log query, found in the portal under Monitor > Workbooks
// as "CivicGrant <env>".
//
// What it shows lives in infra/workbooks/civicgrant.workbook.json, the same format
// the portal's Advanced Editor shows. Every environment loads that one file; only
// the Log Analytics workspace the queries run against differs.
//
// Cost: a workbook has no price of its own, and its queries read tables on the
// Analytics plan, where queries aren't charged.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by environment.bicep.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd. Shown in the workbook\'s title.')
param environmentName string

@description('Azure region of the workbook.')
param location string

@description('Tags applied to the workbook.')
param tags object

@description('Resource id of the Log Analytics workspace the workbook\'s queries run against.')
param workspaceId string


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// The JSON names its workspace with a placeholder, so the file holds no environment's
// id. Bicep reads the file at build time and puts this environment's id in its place.
var content = replace(loadTextContent('../../workbooks/civicgrant.workbook.json'), '__WORKSPACE_ID__', workspaceId)


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The workbook.
resource workbook 'Microsoft.Insights/workbooks@2023-06-01' = {
  // Azure wants a GUID as the name. guid() derives the same one from the same input on
  // every deployment, so a redeploy updates this workbook. newGuid(), as in Microsoft's
  // samples, would add a new copy each time.
  name: guid(resourceGroup().id, 'civicgrant-workbook')
  location: location
  tags: tags
  kind: 'shared'  // Visible to everyone with read access. The only value Azure accepts.
  properties: {
    displayName: 'CivicGrant ${environmentName}'
    category: 'workbook'

    // Lists it in Monitor > Workbooks rather than under one resource only.
    sourceId: 'azure monitor'

    // Must match the "version" inside the JSON.
    version: 'Notebook/1.0'
    serializedData: content
  }
}
