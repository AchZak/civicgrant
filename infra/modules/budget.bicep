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
// Values are passed by shared.bicep.
// ------------------------------------------------------------

@description('Name of the budget, unique within the subscription.')
param name string

@description('Monthly amount, in the billing account\'s currency (EUR for a Belgian account).')
param amount int

@description('Email addresses that receive the alerts.')
param contactEmails array

// The start date is fixed once the budget exists: keep passing the date it was
// created with. A new budget needs the first day of the current month, since Azure
// rejects a past start date outside the current month when creating one.
@description('Start of the budget, the first day of a month, as yyyy-MM-ddT00:00:00Z.')
param startDate string

// Without an end date, Azure sets one 10 years after the start. Writing it down
// keeps what-if clean.
@description('End of the budget, as yyyy-MM-ddT00:00:00Z.')
param endDate string


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
      endDate: endDate
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
