using 'entra.bicep'

// Add acc and prd with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true
    // The Static Web App's root, with the trailing slash: the frontend redirects to document.baseURI.
    spaRedirectUris: [
      'http://localhost:4200/'
      'https://gentle-sea-04341f40f.8.azurestaticapps.net/'
    ]
  }
]
