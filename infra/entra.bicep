// Only gives the deployment a home: everything it creates lives in Entra ID.
targetScope = 'resourceGroup'

@description('One API and one SPA registration per environment.')
param environments {
  @description('Environment name.')
  name: 'tst' | 'acc' | 'prd'

  @description('Let the Azure CLI request tokens for this environment\'s API, to call it by hand. Never in production.')
  allowAzureCli: bool

  @description('Where the frontend may receive sign-in responses: its base URL per language, with the trailing slash.')
  spaRedirectUris: string[]
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

module spas 'modules/entra/spa-registration.bicep' = [
  for (environment, i) in environments: {
    name: 'spa-registration-${environment.name}'
    params: {
      environmentName: environment.name
      redirectUris: environment.spaRedirectUris
      apiClientId: apis[i].outputs.clientId
      apiServicePrincipalId: apis[i].outputs.servicePrincipalId
      apiScopeId: apis[i].outputs.scopeId
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

output spas array = [
  for (environment, i) in environments: {
    name: environment.name
    clientId: spas[i].outputs.clientId
  }
]
