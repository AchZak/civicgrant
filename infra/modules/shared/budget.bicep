targetScope = 'subscription'

@description('Name of the budget, unique within the subscription.')
param name string

@description('Monthly amount, in the billing account\'s currency (EUR for a Belgian account).')
param amount int

@description('Email addresses that receive the alerts.')
param contactEmails array

// Fixed once the budget exists. A new budget needs the first day of the current month.
@description('Start of the budget, the first day of a month, as yyyy-MM-ddT00:00:00Z.')
param startDate string

// Azure sets one 10 years out when it's missing. Declared so what-if stays clean.
@description('End of the budget, as yyyy-MM-ddT00:00:00Z.')
param endDate string

var contactRoles = [
  'Owner'
]

resource budget 'Microsoft.Consumption/budgets@2026-06-01' = {
  name: name
  properties: {
    category: 'Cost'
    amount: amount
    timeGrain: 'Monthly'
    timePeriod: {
      startDate: startDate
      endDate: endDate
    }

    notifications: {
      actualFirstEuro: {
        enabled: true
        thresholdType: 'Actual'
        operator: 'GreaterThan'
        threshold: 20
        contactEmails: contactEmails
        contactRoles: contactRoles
        locale: 'en-us'
      }

      actualFull: {
        enabled: true
        thresholdType: 'Actual'
        operator: 'GreaterThan'
        threshold: 100
        contactEmails: contactEmails
        contactRoles: contactRoles
        locale: 'en-us'
      }

      forecastFull: {
        enabled: true
        thresholdType: 'Forecasted'
        operator: 'GreaterThan'
        threshold: 100
        contactEmails: contactEmails
        contactRoles: contactRoles
        locale: 'en-us'
      }
    }
  }
}
