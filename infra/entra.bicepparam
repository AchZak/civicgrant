using 'entra.bicep'

// Add acc and prd with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true
    // The Static Web App's /nl/ and /fr/ join localhost once it exists.
    spaRedirectUris: [
      'http://localhost:4200/'
    ]
  }
]
