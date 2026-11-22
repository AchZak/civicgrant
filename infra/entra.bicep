// Only gives the deployment a home: everything it creates lives in Entra ID.
targetScope = 'resourceGroup'

@description('One API registration per environment.')
param environments {
  @description('Environment name.')
  name: 'tst' | 'acc' | 'prd'

  @description('Let the Azure CLI request tokens for this environment\'s API, to call it by hand. Never in production.')
  allowAzureCli: bool
}[]

module apis 'modules/entra/api-registration.bicep' = [
  for environment in environments: {
    name: 'api-registration-${environment.name}'
    params: {
      environmentName: environment.name
      allowAzureCli: environment.allowAzureCli
    }
  }
]

output apis array = [
  for (environment, i) in environments: {
    name: environment.name
    clientId: apis[i].outputs.clientId
    identifierUri: apis[i].outputs.identifierUri
    servicePrincipalId: apis[i].outputs.servicePrincipalId
  }
]
