// ============================================================
// MICROSOFT ENTRA ID APPLICATIONS
// ============================================================
//
// Registers the CivicGrant API in the tenant's Microsoft Entra ID, once per
// environment:
//
//   civicgrant-api-<env>        app registration: what the API is
//     api://<tenant id>/civicgrant-api-<env>
//                               Application ID URI: the prefix of its scope
//     access_as_user            scope: clients call the API for a signed-in user
//     APPLICANT, ASSESSOR, DECISION_MAKER
//                               app roles, assigned to users
//     Azure CLI                 pre-authorised client, where allowAzureCli is true
//   service principal           the registration's instance in this tenant: users
//                               without a role assignment on it get no token
//
// Who holds which role isn't declared here: assignments change far more often
// than the API's definition, and would put user ids in this public repository.
// They're granted by hand (see infra/bootstrap).
//
// Deployed by hand, from the project root, signed in as an administrator of the
// tenant (Microsoft Graph resources are created with your own Entra rights):
//
//   az bicep lint --file infra/entra.bicep
//   az deployment group create --resource-group rg-civicgrant-shared --name civicgrant-entra --parameters infra/entra.bicepparam
//
// There's no what-if step: Azure can't preview Microsoft Graph resources, nor put
// them in a Deployment Stack. Review the template's Git diff instead, and query
// the result afterwards (az ad app show).
//
// The pipeline doesn't deploy this file: creating applications needs tenant-wide
// rights that no pipeline identity should hold.
// Running it again is safe: each registration is found again by its uniqueName.


// ------------------------------------------------------------
// SCOPE
// ------------------------------------------------------------

// Deployed into rg-civicgrant-shared only so the deployment has a place in Azure's
// history. It creates nothing in the resource group: everything lives in Entra.
targetScope = 'resourceGroup'


// ------------------------------------------------------------
// PARAMETERS
// Values come from entra.bicepparam.
// ------------------------------------------------------------

@description('One API registration per environment.')
param environments {
  @description('Environment name.')
  name: 'tst' | 'acc' | 'prd'

  @description('Let the Azure CLI request tokens for this environment\'s API, to call it by hand. Never in production.')
  allowAzureCli: bool
}[]


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. One API registration and its service principal per environment.
module apis 'modules/api-registration.bicep' = [
  for environment in environments: {
    name: 'api-registration-${environment.name}'  // Name of the nested deployment, shown in Azure's history.
    params: {
      environmentName: environment.name
      allowAzureCli: environment.allowAzureCli
    }
  }
]


// ------------------------------------------------------------
// OUTPUTS
// Read with az deployment group show. clientId becomes the API_CLIENT_ID
// variable of the environment's GitHub Environment. None of them is a secret.
// ------------------------------------------------------------

output apis array = [
  for (environment, i) in environments: {
    name: environment.name
    clientId: apis[i].outputs.clientId
    identifierUri: apis[i].outputs.identifierUri
    servicePrincipalId: apis[i].outputs.servicePrincipalId
  }
]
