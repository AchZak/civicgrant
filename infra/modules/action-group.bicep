// ============================================================
// MODULE: ACTION GROUP
// ============================================================
//
// Creates the action group every environment's alert rules notify: who gets told,
// and how. Here one email address.
//
// An alert rule only decides when something is wrong; it names action groups to
// say what happens then. Keeping the recipients in one shared group means a new
// address is changed once, not in every environment's rules.
//
// An action group costs nothing by itself. Emails are free up to 1,000 a month.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Name of the action group.')
param name string

@description('Tags applied to the action group.')
param tags object

// Shown in the alert emails and texts instead of the full name.
@description('Short name of the action group, at most 12 characters.')
@maxLength(12)
param shortName string

@description('Email address that receives the alerts.')
param email string


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The action group.
resource actionGroup 'Microsoft.Insights/actionGroups@2023-01-01' = {
  name: name

  // Global: Azure decides where the definition is stored, and it keeps working when
  // a region is down. The guardrails' allowed locations don't apply to global resources.
  location: 'Global'
  tags: tags
  properties: {
    groupShortName: shortName
    enabled: true

    emailReceivers: [
      {
        name: 'alert-email'  // Identifies the receiver inside the group.
        emailAddress: email

        // One JSON layout for every kind of alert (metric, log, activity log), so
        // anything reading the emails or a later webhook parses a single format.
        useCommonAlertSchema: true
      }
    ]
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output id string = actionGroup.id      // Referenced by every environment's alert rules.
output name string = actionGroup.name  // For the deploy identities' read right on it.
