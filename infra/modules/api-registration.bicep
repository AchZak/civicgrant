// ============================================================
// MODULE: API REGISTRATION
// ============================================================
//
// Registers one environment's CivicGrant API in Microsoft Entra ID.
//
// Two objects, like a class and its instance:
//   - the application (app registration) describes the API: its address, the
//     scope clients ask for, its roles and the kind of token it accepts;
//   - the service principal is that application instantiated in this tenant.
//     Role assignments attach to it, and Entra checks it before issuing a token.
//
// How a user reaches the API:
//   1. A client (later the Angular app, for now the Azure CLI) asks Entra for a
//      token for scope api://<tenant id>/civicgrant-api-<env>/access_as_user.
//   2. Entra checks the user holds one of the app roles below, then issues a
//      token with aud = this application's client id, scp = access_as_user and
//      roles = the user's roles.
//   3. The API checks the signature, issuer, audience and scope, and reads oid
//      (who) and roles (what they may do).
//
// These resources come from the Microsoft Graph extension (bicepconfig.json),
// not from Azure Resource Manager: they live in the tenant, not in a subscription.


// ------------------------------------------------------------
// EXTENSIONS
// ------------------------------------------------------------

// Brings in the Microsoft.Graph/* types. The alias and its pinned version are in
// bicepconfig.json.
extension microsoftGraphV1


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by entra.bicep.
// ------------------------------------------------------------

@description('Environment name: tst, acc or prd.')
param environmentName string

@description('Let the Azure CLI request tokens for this API, to call it by hand. Never in production.')
param allowAzureCli bool


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// The registration's name and its Application ID URI path. uniqueName can never
// change once created: it's how every later deployment finds this registration.
var name = 'civicgrant-api-${environmentName}'

// The one delegated permission (scope) the API exposes. It ends up in the token's
// scp claim, and the API rejects tokens without it: app-only tokens never carry one.
var scopeValue = 'access_as_user'

// Scopes and roles are identified by GUIDs that we choose. guid() derives them
// from fixed strings, so every deployment produces the same ones. Never change
// these seeds: a new GUID means a new role, and Entra refuses to drop the old one
// while it's enabled.
var scopeId = guid('civicgrant-api', environmentName, scopeValue)

// The app roles. Values equal the domain's Role enum (be.achzak.civicgrant.domain.shared.Role):
// the API maps the token's roles claim onto it by name.
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
]

// The Azure CLI's client id: the same in every tenant (a Microsoft first-party app).
var azureCliClientId = '04b07795-8ddb-461a-bbee-02f9e1bf7b46'


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The application: what the API is.
resource application 'Microsoft.Graph/applications@v1.0' = {
  uniqueName: name
  displayName: name

  // Where its configuration lives, shown on the registration's overview page.
  notes: 'Managed by infra/entra.bicep in github.com/AchZak/civicgrant. Change it there, not in the portal.'

  // Users of this tenant only. Other organisations and personal accounts can't sign in.
  signInAudience: 'AzureADMyOrg'

  // The Application ID URI: the prefix of the scope clients ask for. The
  // recommended api://<client id> would need the application to reference itself,
  // which Bicep can't do. api://<tenant id>/<name> is one of the other patterns
  // Entra accepts as safe.
  identifierUris: [
    'api://${tenant().tenantId}/${name}'
  ]

  api: {
    // Version 2 access tokens: issuer https://login.microsoftonline.com/<tenant id>/v2.0
    // and aud = the client id. Without this, Entra issues version 1 tokens, whose
    // issuer and audience have another format.
    requestedAccessTokenVersion: 2

    oauth2PermissionScopes: [
      {
        id: scopeId
        value: scopeValue
        isEnabled: true // To remove a scope: false in one deployment, then delete it in the next.

        // Admin: only an administrator may consent to a client using this scope.
        // Users can't let an unknown app call the API on their behalf.
        type: 'Admin'
        adminConsentDisplayName: 'Use CivicGrant as the signed-in user'
        adminConsentDescription: 'Lets the client call the CivicGrant API on behalf of the signed-in user, within that user\'s roles.'
      }
    ]

    // Clients allowed to use the scope without anyone consenting. The Azure CLI
    // here lets you get a token with az account get-access-token before any
    // frontend exists. Later, the Angular app is added the same way.
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
      value: role.value // What the token's roles claim contains.
      displayName: role.displayName
      description: role.description
      allowedMemberTypes: [
        'User' // People only. Other applications can't be given these roles.
      ]
      isEnabled: true // To remove a role: false in one deployment, then delete it in the next.
    }
  ]
}

// 2. The service principal: the application's instance in this tenant.
resource servicePrincipal 'Microsoft.Graph/servicePrincipals@v1.0' = {
  // Using the application's appId makes Bicep create step 1 first.
  appId: application.appId

  // Only users holding one of the roles get a token at all. Without this, any
  // user of the tenant would get a token with an empty roles claim.
  appRoleAssignmentRequired: true

  // Hides the API from users' My Apps portal: there's nothing to open there, the
  // API is reached through its client.
  tags: [
    'HideApp'
  ]
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output clientId string = application.appId                     // The audience the API checks.
output identifierUri string = application.identifierUris[0]    // Prefix of the scope clients request.
output servicePrincipalId string = servicePrincipal.id         // Role assignments point to it.
