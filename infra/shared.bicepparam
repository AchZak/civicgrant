using 'shared.bicep'

param location = 'belgiumcentral'
param githubSubjectPrefix = 'repo:AchZak@140258958/civicgrant@1422522301'

// Standard is in the 12-month free services, Basic isn't. Switch to Basic before they end.
param registrySku = 'Standard'
