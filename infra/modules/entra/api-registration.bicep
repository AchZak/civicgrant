extension microsoftGraphV1

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Let the Azure CLI request tokens for this API, to call it by hand. Never in production.')
param allowAzureCli bool

// uniqueName can never change: later deployments find the registration by it.
var name = 'civicgrant-api-${environmentName}'

var scopeValue = 'access_as_user'

// Never change these seeds: a new GUID is a new role, and Entra won't drop an enabled one.
var scopeId = guid('civicgrant-api', environmentName, scopeValue)

// Values must match the Role enum in core.
var roles = [
  {
    value: 'APPLICANT'
    displayName: 'Applicant'
    description: 'Creates, submits and follows grant applications for an organisation.'
  }
  {
    value: 'ASSESSOR'
    displayName: 'Assessor'
    description: 'Assesses submitted applications and proposes an amount.'
  }
  {
    value: 'DECISION_MAKER'
    displayName: 'Decision maker'
    description: 'Approves or rejects assessed applications, with a written motivation.'
  }
  {
    value: 'PROGRAMME_MANAGER'
    displayName: 'Programme manager'
    description: 'Opens funding calls, with their dates and budget.'
  }
]

// The Azure CLI, same client id in every tenant.
var azureCliClientId = '04b07795-8ddb-461a-bbee-02f9e1bf7b46'

resource application 'Microsoft.Graph/applications@v1.0' = {
  uniqueName: name
  displayName: name
  notes: 'Managed by infra/entra.bicep in github.com/AchZak/civicgrant. Change it there, not in the portal.'
  signInAudience: 'AzureADMyOrg'

  // api://<client id> would need the app to reference itself, which Bicep can't do.
  identifierUris: [
    'api://${tenant().tenantId}/${name}'
  ]

  api: {
    // v2 tokens: the issuer the API checks ends in /v2.0.
    requestedAccessTokenVersion: 2

    oauth2PermissionScopes: [
      {
        id: scopeId
        value: scopeValue
        isEnabled: true // Remove in two deployments: false first, then delete.
        type: 'Admin'
        adminConsentDisplayName: 'Use CivicGrant as the signed-in user'
        adminConsentDescription: 'Lets the client call the CivicGrant API on behalf of the signed-in user, within that user\'s roles.'
      }
    ]

    preAuthorizedApplications: allowAzureCli
      ? [
          {
            appId: azureCliClientId
            delegatedPermissionIds: [
              scopeId
            ]
          }
        ]
      : []
  }

  appRoles: [
    for role in roles: {
      id: guid('civicgrant-api', environmentName, role.value)
      value: role.value
      displayName: role.displayName
      description: role.description
      allowedMemberTypes: [
        'User'
      ]
      isEnabled: true // Remove in two deployments: false first, then delete.
    }
  ]
}

resource servicePrincipal 'Microsoft.Graph/servicePrincipals@v1.0' = {
  appId: application.appId

  // Without this, any user in the tenant gets a token with an empty roles claim.
  appRoleAssignmentRequired: true

  tags: [
    'HideApp'
  ]
}

output clientId string = application.appId
output identifierUri string = application.identifierUris[0]
output servicePrincipalId string = servicePrincipal.id
