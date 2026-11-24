extension microsoftGraphV1

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Where Entra may send the user back after sign-in and sign-out: the frontend\'s base URL per language, with its trailing slash.')
param redirectUris string[]

@description('Client id of the environment\'s API registration.')
param apiClientId string

@description('Object id of the API\'s service principal.')
param apiServicePrincipalId string

@description('Id of the API\'s access_as_user scope.')
param apiScopeId string

// uniqueName can never change: later deployments find the registration by it.
var name = 'civicgrant-spa-${environmentName}'

var microsoftGraphAppId = '00000003-0000-0000-c000-000000000000'

// Microsoft Graph's delegated permissions for sign-in, the same ids in every tenant.
var signInScopes = [
  {
    value: 'openid'
    id: '37f7f235-527c-4136-accd-4a02d197296e'
  }
  {
    value: 'profile'
    id: '14dad69e-099b-42c9-810b-d002981feec1'
  }
  {
    value: 'offline_access'
    id: '7427e0e9-2fba-42fe-b0c0-848c9e6a8182'
  }
]

resource application 'Microsoft.Graph/applications@v1.0' = {
  uniqueName: name
  displayName: name
  notes: 'Managed by infra/entra.bicep in github.com/AchZak/civicgrant-backend. Change it there, not in the portal.'
  signInAudience: 'AzureADMyOrg'

  spa: {
    redirectUris: redirectUris
  }

  requiredResourceAccess: [
    {
      resourceAppId: apiClientId
      resourceAccess: [
        {
          id: apiScopeId
          type: 'Scope'
        }
      ]
    }
    {
      resourceAppId: microsoftGraphAppId
      resourceAccess: [
        for scope in signInScopes: {
          id: scope.id
          type: 'Scope'
        }
      ]
    }
  ]
}

resource servicePrincipal 'Microsoft.Graph/servicePrincipals@v1.0' = {
  appId: application.appId
}

resource microsoftGraph 'Microsoft.Graph/servicePrincipals@v1.0' existing = {
  appId: microsoftGraphAppId
}

// Consent for every user of the tenant: the API's scope is admin-only, so nobody could accept a prompt for it.
resource apiConsent 'Microsoft.Graph/oauth2PermissionGrants@v1.0' = {
  clientId: servicePrincipal.id
  consentType: 'AllPrincipals'
  resourceId: apiServicePrincipalId
  scope: 'access_as_user'
}

resource signInConsent 'Microsoft.Graph/oauth2PermissionGrants@v1.0' = {
  clientId: servicePrincipal.id
  consentType: 'AllPrincipals'
  resourceId: microsoftGraph.id
  scope: join(map(signInScopes, scope => scope.value), ' ')
}

output clientId string = application.appId
