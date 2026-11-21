# Bootstrap: app roles

Gives a user one of the CivicGrant API's app roles (`APPLICANT`, `ASSESSOR`, `DECISION_MAKER`, `PROGRAMME_MANAGER`) in one environment. Without a role, Entra issues that user no token for the API at all: the API's service principal requires an assignment (`appRoleAssignmentRequired` in `infra/modules/api-registration.bicep`).

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

## Test users

Separation of duties means one person can't assess and decide on the same application, so a smoke test on tst needs more than one staff account. tst has two, `civicgrant-tst-staff-a` and `civicgrant-tst-staff-b`, both with `ASSESSOR` and `DECISION_MAKER`: staff A assesses, staff B decides. Your own account keeps all three roles, which is how the API's `conflictOfInterest` shows up: you can't assess or decide on an application you submitted yourself.

They're members of the tenant's own `onmicrosoft.com` domain, which Entra ID Free allows at no cost. Creating them needs User Administrator or Global Administrator.

### 1. Create a user

```bash
env=tst
letter=a
domain=$(az rest --method GET --url "https://graph.microsoft.com/v1.0/domains" \
  --query "value[?isDefault].id | [0]" --output tsv | tr -d '\r')
nickname="civicgrant-$env-staff-$letter"

# The password is hidden while you type and ends up nowhere but in Entra.
read -rsp "Password for $nickname: " password; echo
az ad user create \
  --display-name "CivicGrant $env staff ${letter^^}" \
  --user-principal-name "$nickname@$domain" \
  --mail-nickname "$nickname" \
  --password "$password" \
  --force-change-password-next-sign-in false \
  --query id --output tsv
unset password
```

Then give the user `ASSESSOR` and `DECISION_MAKER` with steps 1 and 3 above, taking `user_id` from `az ad user show --id "$nickname@$domain" --query id --output tsv`.

### 2. Sign the user in once

The API only accepts tokens, and the Azure CLI only hands out a token for an account that has signed in. Each test user gets its own CLI folder (`AZURE_CONFIG_DIR`), so your own sign-in stays untouched.

Run this for one user at a time, in your own Git Bash window:

```bash
export AZURE_CONFIG_DIR="$HOME/.azure-$nickname"
# First thing, before any other az command in this folder: see below.
az config set core.enable_broker_on_windows=false
az login --tenant "<tenant id>" --allow-no-subscriptions
unset AZURE_CONFIG_DIR
```

The browser opens. Choose "Use another account" and type the **full** name, `civicgrant-tst-staff-a@<domain>`. Entra then asks to set up Microsoft Authenticator: add it in the app as a **"Work or school account"**. This tenant uses security defaults, so every account registers for MFA on its first sign-in, with no grace period.

After one user, sign out of Microsoft in the browser before the next (`https://login.microsoftonline.com/common/oauth2/v2.0/logout`). Otherwise the browser signs the previous account in silently, and B's folder ends up holding A.

Check that the folder holds the right account:

```bash
AZURE_CONFIG_DIR="$HOME/.azure-$nickname" az account show --query user.name --output tsv
```

### 3. Use it

```bash
token=$(AZURE_CONFIG_DIR="$HOME/.azure-civicgrant-tst-staff-a" az account get-access-token \
  --scope "api://<tenant id>/civicgrant-api-tst/access_as_user" --query accessToken --output tsv | tr -d '\r')
```

Check its roles with step 4 above.

### Why it's done this way

These were all hit on 18 November 2026.

- **The Windows broker goes off first.** On Windows the Azure CLI signs in through the Web Account Manager by default, and that list of accounts is shared by every CLI folder. With it on, the sign-in offers your own account, and `az account clear` in a test user's folder signs your own account out everywhere. If your main CLI ever needs it back: `az config set core.enable_broker_on_windows=true`.
- **No device code sign-in.** Security defaults block the device code flow (`AADSTS530035`), so `az login --use-device-code` fails. The browser sign-in works.
- **The full name, with the domain.** Without `@<domain>`, Microsoft takes it for a personal account and answers "That Microsoft account doesn't exist" (`AADSTS900561`).
- **One user per run, signed out in between.** The browser remembers the last account and reuses it without asking.
- **Your own terminal.** A terminal tab opened by a tool may not take your typing; run the commands in a window you opened yourself.

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
| `AADSTS530035` on `az login --use-device-code` | Security defaults block the device code flow: sign in through the browser |
| `AADSTS900561` or "That Microsoft account doesn't exist" | The name was typed without `@<domain>`, so it went to personal accounts |
| A test user's folder holds the wrong account | The browser reused the previous sign-in: sign out (see "Test users", step 2) and sign in again |
| The API answers 401 `invalid_token` | Compare `aud` and `iss` with the table in step 4. A `ver` of `1.0` means the registration's `requestedAccessTokenVersion` isn't 2 |
