@description('Name of the action group.')
param name string

@description('Tags applied to the action group.')
param tags object

@description('Short name of the action group, at most 12 characters.')
@maxLength(12)
param shortName string

@description('Email address that receives the alerts.')
param email string

resource actionGroup 'Microsoft.Insights/actionGroups@2023-01-01' = {
  name: name
  location: 'Global'
  tags: tags
  properties: {
    groupShortName: shortName
    enabled: true

    emailReceivers: [
      {
        name: 'alert-email'
        emailAddress: email
        useCommonAlertSchema: true
      }
    ]
  }
}

output id string = actionGroup.id
output name string = actionGroup.name
