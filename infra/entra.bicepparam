using 'entra.bicep'

// Add acc and prd with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true
  }
]
