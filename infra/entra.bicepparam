using 'entra.bicep'

// Add acc and prd with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true
    // Per host: MSAL's redirect bridge page, where sign-in returns, and the root, where sign-out returns.
    spaRedirectUris: [
      'http://localhost:4200/auth/redirect.html'
      'http://localhost:4200/'
      'https://gentle-sea-04341f40f.8.azurestaticapps.net/auth/redirect.html'
      'https://gentle-sea-04341f40f.8.azurestaticapps.net/'
    ]
  }
]
