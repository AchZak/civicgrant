// Static Web Apps aren't offered in the regions the guardrails allow, so they may use another one.
// Exemptions have no stable API version.
resource locationExemption 'Microsoft.Authorization/policyExemptions@2024-12-01-preview' = {
  name: 'civicgrant-guardrails-static-web-apps'
  properties: {
    policyAssignmentId: resourceId('Microsoft.Authorization/policyAssignments', 'civicgrant-guardrails')
    policyDefinitionReferenceIds: [
      'allowedLocations'
    ]
    exemptionCategory: 'Waiver'
    displayName: 'Static Web Apps outside the allowed regions'
    description: 'Static Web Apps are not offered in Belgium or France Central. Only their metadata lives in their region; the files are served from the worldwide edge and hold no personal data.'
    resourceSelectors: [
      {
        name: 'staticWebApps'
        selectors: [
          {
            kind: 'resourceType'
            in: [
              'Microsoft.Web/staticSites'
            ]
          }
        ]
      }
    ]
  }
}
