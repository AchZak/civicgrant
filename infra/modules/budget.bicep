// ============================================================
// MODULE: BUDGET
// ============================================================
//
// Creates a monthly cost budget on the whole subscription, which emails
// alerts as spending approaches or passes the amount.
//
// A budget only warns. It never stops or deletes resources, and Azure's cost
// data lags 8 to 24 hours behind actual usage, so an alert is an early warning,
// not a hard limit.

// A budget belongs to the subscription itself, not to a resource group.
targetScope = 'subscription'


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by shared.bicep, except startDate.
// ------------------------------------------------------------

@description('Name of the budget, unique within the subscription.')
param name string

@description('Monthly amount, in the billing account\'s currency (EUR for a Belgian account).')
param amount int

@description('Email addresses that receive the alerts.')
param contactEmails array

// The first day of the current month, computed when the deployment runs.
// Azure requires a start date on the first of a month, and a past start date must
// fall within the current month. A fixed date would make every redeploy fail from
// the next month on. utcNow() is only allowed as a parameter default.
@description('Start of the budget, the first day of a month.')
param startDate string = '${utcNow('yyyy-MM')}-01T00:00:00Z'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The budget.
resource budget 'Microsoft.Consumption/budgets@2026-06-01' = {
  name: name
  properties: {
    category: 'Cost'     // Tracks money spent, not usage quantities.
    amount: amount
    timeGrain: 'Monthly' // The tracked amount resets on the first of each month.
    timePeriod: {
      startDate: startDate
    }

    // Up to five alerts. Each threshold is a percentage of the amount.
    //   Actual:     costs already recorded this month.
    //   Forecasted: Azure's projection for the full month, based on the current trend.
    //               It can warn before the money is spent.
    // The keys (actualFirstEuro…) are free names that identify each alert.
    notifications: {
      // The first euro of the month. On a project meant to cost nothing, any spend
      // is worth knowing about.
      actualFirstEuro: {
        enabled: true
        thresholdType: 'Actual'
        operator: 'GreaterThan'
        threshold: 20 // 20% of 5 = 1
        contactEmails: contactEmails
        locale: 'en-us'
      }

      // The whole amount has been spent this month.
      actualFull: {
        enabled: true
        thresholdType: 'Actual'
        operator: 'GreaterThan'
        threshold: 100
        contactEmails: contactEmails
        locale: 'en-us'
      }

      // At the current pace, the month will end above the amount.
      forecastFull: {
        enabled: true
        thresholdType: 'Forecasted'
        operator: 'GreaterThan'
        threshold: 100
        contactEmails: contactEmails
        locale: 'en-us'
      }
    }
  }
}
