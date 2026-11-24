using 'entra.bicep'

// Add acc and prd with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true
    // The Static Web App's root, https://<host>/, joins localhost once it exists.
    spaRedirectUris: [
      'http://localhost:4200/'
    ]
  }
]
