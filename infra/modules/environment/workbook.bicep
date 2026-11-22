@description('Environment name: tst, acc or prd. Shown in the workbook\'s title.')
param environmentName string

@description('Azure region of the workbook.')
param location string

@description('Tags applied to the workbook.')
param tags object

@description('Resource id of the Log Analytics workspace the workbook\'s queries run against.')
param workspaceId string

var content = replace(loadTextContent('../../workbooks/civicgrant.workbook.json'), '__WORKSPACE_ID__', workspaceId)

resource workbook 'Microsoft.Insights/workbooks@2023-06-01' = {
  // guid(), not newGuid() as in Microsoft's samples: a redeploy must update this workbook.
  name: guid(resourceGroup().id, 'civicgrant-workbook')
  location: location
  tags: tags
  kind: 'shared'
  properties: {
    displayName: 'CivicGrant ${environmentName}'
    category: 'workbook'
    sourceId: 'azure monitor'

    // Must match "version" in the JSON.
    version: 'Notebook/1.0'
    serializedData: content
  }
}
