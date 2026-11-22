// ============================================================
// MODULE: GUARDRAILS (POLICY INITIATIVE)
// ============================================================
//
// Defines the project's rules as one Azure Policy initiative, a named group of
// built-in policy definitions:
//
//   civicgrant-guardrails      policy set definition, at subscription level
//     11 rules with Deny       Azure refuses a request that breaks one of them
//     1 rule per tag (Modify)  a resource without the tag inherits it from its group
//
// Defining it changes nothing: rules only apply where the initiative is assigned
// (guardrails-assignment.bicep), which is every CivicGrant resource group and
// nothing else in the subscription.
//
// Azure Policy costs nothing for Azure resources.


// ------------------------------------------------------------
// SCOPE
// ------------------------------------------------------------

// Definitions live at subscription (or management group) level, so every resource
// group in the subscription can be assigned the same one.
targetScope = 'subscription'


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Azure regions where CivicGrant resources may be created.')
param allowedLocations array

@description('Tags every resource inherits from its resource group when it doesn\'t set them itself.')
param inheritedTagNames array


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// Ids of the built-in definitions, the same in every tenant, each pinned to its current
// major version: Microsoft's fixes within that major apply by themselves, a new major
// (which may change what the rule checks) only when its version here is raised.
// To look one up: az policy definition list --query "[?displayName=='<name>'].{id:name, version:version}"
var denyRules = [
  {
    referenceId: 'allowedLocations'
    definitionId: 'e56962a6-4747-49cd-b67b-bf8b01975c4c'  // Allowed locations
    version: '1.*.*'
    parameters: {
      listOfAllowedLocations: { value: allowedLocations }
    }
  }
  {
    referenceId: 'storageNoSharedKey'
    definitionId: '8c6a50c6-9ffd-4ae7-986f-5fa6111f9a54'  // Storage accounts should prevent shared key access
    version: '2.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageNoPublicAccess'
    definitionId: '4fa4b6c0-31ca-4c0d-b10d-24b96f62a751'  // Storage account public access should be disallowed
    version: '3.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageHttpsOnly'
    definitionId: '404c3081-a854-4457-ae30-26a93ef643f9'  // Secure transfer to storage accounts should be enabled
    version: '2.*.*'
    parameters: {}
  }
  {
    referenceId: 'storageMinimumTls12'
    definitionId: 'fe83a0eb-a853-422d-aac2-1bffd182c5d0'  // Storage accounts should have the specified minimum TLS version
    version: '1.*.*'
    parameters: {
      minimumTlsVersion: { value: 'TLS1_2' }
    }
  }
  {
    referenceId: 'registryNoAdminUser'
    definitionId: 'dc921057-6b28-4fbe-9b83-f7bec05db6c2'  // Container registries should have local admin account disabled
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'registryNoAnonymousPull'
    definitionId: '9f2dea28-e834-476c-99c5-3507b4728395'  // Container registries should have anonymous authentication disabled
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'containerAppHttpsOnly'
    definitionId: '0e80e269-43a4-4ae9-b5bc-178126b8a5cb'  // Container Apps should only be accessible over HTTPS
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'containerAppManagedIdentity'
    definitionId: 'b874ab2d-72dd-47f1-8cb5-4a306478a4e7'  // Managed Identity should be enabled for Container Apps
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'logAnalyticsEntraOnly'
    definitionId: 'e15effd4-2278-4c65-a0da-4d6f6d1890e2'  // Log Analytics Workspaces should block non-Azure Active Directory based ingestion
    version: '1.*.*'
    parameters: {}
  }
  {
    referenceId: 'applicationInsightsEntraOnly'
    definitionId: '199d5677-e4d9-4264-9465-efe1839c06bd'  // Application Insights components should block non-Azure Active Directory based ingestion
    version: '1.*.*'
    parameters: {}
  }
]

// Inherit a tag from the resource group if missing. Its effect is Modify and can't be
// changed: on a create or update, Azure adds the missing tag before the request
// reaches the resource provider. Mostly for resources Azure creates on its own, such
// as the Smart Detection action group of Application Insights, which carry no tags.
var inheritTagDefinitionId = 'ea3f2387-9b95-492a-a190-fcdc54f7b070'
var inheritTagVersion = '1.*.*'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The initiative. Each entry points at a built-in definition by id and fills in
//    its parameters. The reference id names the entry inside the initiative: the
//    compliance view shows it, and a remediation task picks one rule by it.
resource guardrails 'Microsoft.Authorization/policySetDefinitions@2026-07-01' = {
  name: 'civicgrant-guardrails'
  properties: {
    displayName: 'CivicGrant guardrails'
    description: 'The rules every CivicGrant resource follows: EU regions, no keys or public access, HTTPS only, Entra-only telemetry, and the project tags.'
    policyType: 'Custom'
    // The initiative's own version. The assignments follow its major (1.*.*), so raise
    // the major when a change should be reviewed before it applies.
    version: '1.0.0'
    metadata: {
      category: 'CivicGrant'
    }
    policyDefinitions: concat(
      map(denyRules, rule => {
        policyDefinitionReferenceId: rule.referenceId
        policyDefinitionId: tenantResourceId('Microsoft.Authorization/policyDefinitions', rule.definitionId)
        definitionVersion: rule.version
        // Every rule here is enforced: Deny, never Audit. A rule worth writing down is
        // worth refusing the request for.
        parameters: union(rule.parameters, { effect: { value: 'Deny' } })
      }),
      map(inheritedTagNames, tagName => {
        policyDefinitionReferenceId: 'inheritTag-${tagName}'
        policyDefinitionId: tenantResourceId('Microsoft.Authorization/policyDefinitions', inheritTagDefinitionId)
        definitionVersion: inheritTagVersion
        parameters: {
          tagName: { value: tagName }
        }
      })
    )
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output id string = guardrails.id
