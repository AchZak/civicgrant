# Bootstrap: app roles

Gives a user one of the CivicGrant API's app roles (`APPLICANT`, `ASSESSOR`, `DECISION_MAKER`) in one environment. Without a role, Entra issues that user no token for the API at all: the API's service principal requires an assignment (`appRoleAssignmentRequired` in `infra/modules/api-registration.bicep`).

Bicep declares the roles, not who holds them: assignments change far more often than the API, and would put user ids in this public repository. Assigning a group instead of each user would need Microsoft Entra ID P1, a paid licence, so users are assigned one by one, which Entra ID Free allows.

## Who and where

- A tenant administrator (Global Administrator, Application Administrator or Cloud Application Administrator), signed in to the Azure CLI.
- From Git Bash on your own machine: this only calls Microsoft Graph, a public endpoint, so no firewall is involved.
- `infra/entra.bicep` must have been deployed for the environment (deployment `civicgrant-entra` in `rg-civicgrant-shared`).

## Steps

### 1. Read the values

Every value comes from Entra or from the deployment's outputs. Change `env`, `role` and the user for another assignment.

```bash
env=tst
role=ASSESSOR

# The API's service principal: role assignments attach to it.
sp_id=$(az deployment group show --resource-group rg-civicgrant-shared --name civicgrant-entra \
  --query "properties.outputs.apis.value[?name=='$env'].servicePrincipalId | [0]" --output tsv | tr -d '\r')

# The role's id, declared in the app registration.
role_id=$(az ad sp show --id "$sp_id" \
  --query "appRoles[?value=='$role'].id | [0]" --output tsv | tr -d '\r')

# The user: yourself here. For someone else: az ad user show --id <user principal name> --query id --output tsv
user_id=$(az ad signed-in-user show --query id --output tsv | tr -d '\r')

printf 'service principal: %s\nrole %s:  %s\nuser:              %s\n' "$sp_id" "$role" "$role_id" "$user_id"
```

All three lines must show a GUID. None of them is a secret.

### 2. See who holds which role

```bash
az rest --method GET \
  --uri "https://graph.microsoft.com/v1.0/servicePrincipals/$sp_id/appRoleAssignedTo" \
  --query "value[].{user:principalDisplayName, userId:principalId, roleId:appRoleId, assignment:id}" \
  --output table
```

Compare `roleId` with the ids from `az ad sp show --id "$sp_id" --query "appRoles[].{role:value, roleId:id}" --output table`.

### 3. Assign the role

```bash
az rest --method POST \
  --uri "https://graph.microsoft.com/v1.0/servicePrincipals/$sp_id/appRoleAssignedTo" \
  --headers "Content-Type=application/json" \
  --body "{\"principalId\":\"$user_id\",\"resourceId\":\"$sp_id\",\"appRoleId\":\"$role_id\"}"
```

Graph answers with the new assignment. One assignment per role: repeat steps 1 and 3 with another `role` to give the same user a second one. Assigning a role the user already holds fails with "Permission being assigned already exists on the object", and changes nothing.

### 4. Check it with a real token

Tokens issued from now on carry the role. The Azure CLI may be pre-authorised for this environment's API (`allowAzureCli` in `infra/entra.bicepparam`, tst only), so it can request a token without a consent prompt.

```bash
tenant_id=$(az account show --query tenantId --output tsv | tr -d '\r')
token=$(az account get-access-token \
  --scope "api://$tenant_id/civicgrant-api-$env/access_as_user" \
  --query accessToken --output tsv | tr -d '\r')
```

Read its claims on your machine. A token is three base64url parts separated by dots; the middle one is the payload. Never paste a token into a website: until it expires, it lets anyone act as you on the API.

```bash
payload=$(cut -d. -f2 <<<"$token" | tr '_-' '/+')
while (( ${#payload} % 4 )); do payload+='='; done
base64 -d <<<"$payload"; echo
```

Check:

| Claim | Expected |
|---|---|
| `aud` | the API's client id (`API_CLIENT_ID` of the GitHub Environment) |
| `iss` | `https://login.microsoftonline.com/<tenant id>/v2.0` |
| `ver` | `2.0` |
| `scp` | `access_as_user` |
| `roles` | the roles you assigned |
| `oid` | `$user_id` |

The Azure CLI caches tokens until they expire (about an hour). If the roles look out of date, sign in again (`az login --tenant <tenant id>`) and request a new token.

### 5. Clean up

```bash
unset token payload
```

## Removing a role

Take the assignment's id from step 2, then:

```bash
az rest --method DELETE \
  --uri "https://graph.microsoft.com/v1.0/servicePrincipals/$sp_id/appRoleAssignedTo/<assignment id>"
```

Tokens already issued keep the role until they expire: the API doesn't look anything up, it trusts the token.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `AADSTS50105` when requesting a token | The user holds no role on this environment's API. Assign one (step 3) |
| `AADSTS65001` (consent required) | The Azure CLI isn't pre-authorised for this environment: `allowAzureCli` is false, or `entra.bicep` wasn't redeployed after changing it |
| `AADSTS500011` (resource not found) | Wrong scope: check the tenant id and the environment in `api://<tenant id>/civicgrant-api-<env>` |
| `Insufficient privileges` on step 3 | You're not one of the administrator roles above |
| The API answers 401 `invalid_token` | Compare `aud` and `iss` with the table in step 4. A `ver` of `1.0` means the registration's `requestedAccessTokenVersion` isn't 2 |
