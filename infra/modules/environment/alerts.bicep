@description('Environment name: tst, acc or prd. Part of the rule names.')
param environmentName string

@description('Tags applied to the alert rules.')
param tags object

@description('Resource id of the environment\'s Application Insights: what the rules watch.')
param applicationInsightsId string

@description('Resource id of the shared action group the rules notify.')
param actionGroupId string

// 5xx only: the agent counts every 4xx as failed, and those are refusals by design.
resource serverErrorsAlert 'Microsoft.Insights/metricAlerts@2026-01-01' = {
  name: 'alert-civicgrant-${environmentName}-server-errors'
  location: 'global'
  tags: tags
  properties: {
    description: 'The app answered at least one request with 500 or 503 in the last 15 minutes.'
    severity: 1
    enabled: true
    scopes: [
      applicationInsightsId
    ]

    evaluationFrequency: 'PT5M'
    windowSize: 'PT15M'
    autoMitigate: true

    criteria: {
      'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
      allOf: [
        {
          criterionType: 'StaticThresholdCriterion'
          name: 'server-errors'
          metricNamespace: 'microsoft.insights/components'
          metricName: 'requests/count'
          timeAggregation: 'Count'
          operator: 'GreaterThan'
          threshold: 0

          // 502 and 504 come from the ingress, never the app. Each code is a billed time series.
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
