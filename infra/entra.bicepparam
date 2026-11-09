// ============================================================
// PARAMETER VALUES FOR entra.bicep
// ============================================================
//
// "using" names the template these values belong to, so the deploy command
// only needs --parameters infra/entra.bicepparam.

using 'entra.bicep'

// One registration per environment: a token for tst is rejected by acc and prd,
// and someone can hold a role in tst without holding it in production.
// Add acc and prd when those environments are built, with allowAzureCli false.
param environments = [
  {
    name: 'tst'
    allowAzureCli: true // Test by hand with az account get-access-token.
  }
]
