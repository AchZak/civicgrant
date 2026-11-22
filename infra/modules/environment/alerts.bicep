// ============================================================
// MODULE: ALERTS
// ============================================================
//
// Creates the alert rules of one environment:
//
//   server errors alert   emails when the app answers a request with 500 or 503
//
// The rule watches a metric of the environment's Application Insights and notifies
// the shared action group (ag-civicgrant-shared, created by shared.bicep).
//
// Why 5xx and not "failed requests": the agent marks every 4xx as failed, and a 4xx
// here is a refusal by design (validation, roles, conflicts). A 5xx is always a bug
// or an outage, so a single one is worth an email.
//
// Cost: a metric alert is billed per time series it watches. The first 10 a month
// are free, then about 0.09 euro per series a month. This rule watches 2 (one per
// result code), so three environments stay at 6.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by environment.bicep.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd. Part of the rule names.')
param environmentName string

@description('Tags applied to the alert rules.')
param tags object

@description('Resource id of the environment\'s Application Insights: what the rules watch.')
param applicationInsightsId string

@description('Resource id of the shared action group the rules notify.')
param actionGroupId string


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. Any 500 or 503 answered by the app in the last 15 minutes.
//    Cloud Adoption Framework has no abbreviation for alert rules, so the name
//    starts with alert- and says what it watches.
resource serverErrorsAlert 'Microsoft.Insights/metricAlerts@2026-01-01' = {
  name: 'alert-civicgrant-${environmentName}-server-errors'

  // Global, like the action group: the rule isn't tied to a region. The guardrails'
  // allowed locations don't apply to global resources.
  location: 'global'
  tags: tags
  properties: {
    description: 'The app answered at least one request with 500 or 503 in the last 15 minutes.'

    // 0 critical, 1 error, 2 warning, 3 informational, 4 verbose. A 5xx is an error
    // a user ran into, not yet an outage.
    severity: 1
    enabled: true
    scopes: [
      applicationInsightsId
    ]

    // Every 5 minutes, Azure counts the matching requests of the last 15 minutes. One
    // 500 therefore keeps the alert raised for 15 minutes, not a single check.
    evaluationFrequency: 'PT5M'
    windowSize: 'PT15M'

    // Azure resolves the alert by itself once a window has no 5xx left, and sends a
    // "resolved" email: nobody has to close it by hand.
    autoMitigate: true

    criteria: {
      // One resource, one or more metric conditions: the type for a single
      // Application Insights resource.
      'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
      allOf: [
        {
          criterionType: 'StaticThresholdCriterion'  // A fixed threshold, not one learned from history.
          name: 'server-errors'
          metricNamespace: 'microsoft.insights/components'
          metricName: 'requests/count'  // Requests the agent saw, counted per result code below.
          timeAggregation: 'Count'      // The only aggregation this metric supports.
          operator: 'GreaterThan'
          threshold: 0                  // A count, not a rate: every 5xx is a bug.

          // Only these result codes. 502 and 504 are answered by the Container Apps
          // ingress when the app doesn't respond, so the agent never sees them. Each
          // value is a separate time series, billed on its own: list codes, never a pattern.
          dimensions: [
            {
              name: 'request/resultCode'
              operator: 'Include'
              values: [
                '500'
                '503'
              ]
            }
          ]
        }
      ]
    }

    actions: [
      {
        actionGroupId: actionGroupId
      }
    ]
  }
}
