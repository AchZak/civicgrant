# Bootstrap: database roles

What Bicep can't do, run by hand. This page covers PostgreSQL roles; [app-roles.md](app-roles.md) covers who holds which role on the API.

`database-role.sql` lets an environment's application sign in to the shared PostgreSQL server with its managed identity, and reach its own database only. It creates the role `id-civicgrant-app-<env>`, mapped to the identity's object id, and makes `civicgrant_<env>` reachable by that role alone. The application's Flyway then creates the `civicgrant` schema on its first start and owns it.

Bicep can't do this: database roles live inside PostgreSQL, not in Azure Resource Manager. And the pipeline can't, because no administrator credential exists in CI on purpose. So the server's Entra administrator runs it by hand.

## When to run it

| Situation | Run it? |
|---|---|
| A new environment (its identities and database come from `shared.bicep`) | Yes |
| The server was recreated | Yes, for every environment |
| An app identity was deleted and recreated: it has a new object id | Yes, for that environment. The script maps the existing role to the new id and keeps its data |
| The environment's resource group was deleted and redeployed | No: the identity, the database and the role live in `rg-civicgrant-shared` and survive |
| Nothing changed | Harmless: it reports "unchanged" |

## Why Azure Cloud Shell

The server's firewall only admits connections from Azure (the `0.0.0.0` rule). A rule for your own IP address could be added, but the delete lock on the server would also stop you deleting it afterwards, so it would stay open for good. Cloud Shell runs inside Azure, so the existing rule already admits it.

Use an **ephemeral session** (no storage account): it's free, and everything in it is deleted when the session ends, including the token.

## Once per subscription

Cloud Shell needs its resource provider registered. From Git Bash on your machine:

```bash
az provider register --namespace Microsoft.CloudShell --wait
az provider show --namespace Microsoft.CloudShell --query registrationState --output tsv
```

The second command prints `Registered`.

## Steps

The commit you run must be pushed to `develop` first: Cloud Shell gets the script from GitHub.

### 1. Open Cloud Shell

In the Azure portal, open Cloud Shell from the top bar, choose **Bash**, then **No storage account required** and the `achzak-personal` subscription. Check you're in the right tenant and subscription:

```bash
az account show --query "{tenant:tenantId, subscription:name, user:user.name}" --output table
```

### 2. Check psql

```bash
psql --version
```

The script needs psql 10 or later. Step 5 verifies the server's certificate with the system's certificate store (`sslrootcert=system`), which needs psql 16 or later; with an older psql, use `PGSSLMODE=require` and leave out `PGSSLROOTCERT`.

### 3. Get the script

```bash
git clone --depth 1 --branch develop https://github.com/AchZak/civicgrant.git
cd civicgrant
git log -1 --oneline
```

The last command shows the commit you're about to run: check it's the one you pushed.

### 4. Read the values from Azure

Every value comes from Azure, so nothing is typed by hand. Change `env` for another environment.

```bash
env=tst

server=$(az deployment sub show --name civicgrant-shared --query properties.outputs.postgresServerName.value --output tsv)
host=$(az deployment sub show --name civicgrant-shared --query properties.outputs.postgresHost.value --output tsv)
app_object_id=$(az deployment sub show --name civicgrant-shared \
  --query "properties.outputs.environments.value[?name=='$env'].appPrincipalId | [0]" --output tsv)

# Your role name in PostgreSQL is exactly the administrator's principal name on the server.
admin=$(az postgres flexible-server microsoft-entra-admin list \
  --resource-group rg-civicgrant-shared --server-name "$server" \
  --query "[0].principalName || [0].properties.principalName" --output tsv)

printf 'server: %s\nhost:   %s\nadmin:  %s\napp id: %s\n' "$server" "$host" "$admin" "$app_object_id"
```

All four lines must show a value. None of them is a secret.

### 5. Sign in with your Entra token

```bash
export PGHOST="$host"
export PGUSER="$admin"
export PGDATABASE=postgres
export PGSSLMODE=verify-full
export PGSSLROOTCERT=system
export PGPASSWORD=$(az account get-access-token --resource-type oss-rdbms --query accessToken --output tsv)
```

`PGPASSWORD` holds an access token for Azure Database for PostgreSQL, valid 5 to 60 minutes. Never print it. Environment variables avoid quoting the user name, which contains `#` and `@`. `PGDATABASE=postgres`: roles belong to the whole server, and so do the rights on a database, so the script runs entirely from the `postgres` database.

`verify-full` checks that the server's certificate is valid and matches its name, so the token can't be handed to an impostor: it would let anyone act as you on this server until it expires.

### 6. Run it

```bash
psql \
  -v app_role="id-civicgrant-app-$env" \
  -v app_object_id="$app_object_id" \
  -v database="civicgrant_$env" \
  -f infra/bootstrap/database-role.sql
```

It ends with `Done: ...`. Run it a second time: it reports `unchanged` and still ends with `Done`.

### 7. Check the result

```bash
psql -c "SELECT * FROM pg_catalog.pgaadauth_list_principals(false);"
psql -c "SELECT datname, pg_get_userbyid(datdba) AS owner, datacl FROM pg_database WHERE datname = 'civicgrant_$env';"
```

- The first lists `id-civicgrant-app-<env>` with type `service` and the identity's object id, next to your own administrator role.
- In the second, `datacl` holds one entry per grantee, as `grantee=privileges/grantor`. It must contain `"id-civicgrant-app-<env>"=Cc/...` (`C` create, `c` connect), and no entry starting with `=`: an empty grantee means PUBLIC.

### 8. Clean up

```bash
unset PGPASSWORD
```

Then close Cloud Shell. The ephemeral session, the clone and the token are deleted with it.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Connection timed out` | The firewall didn't admit Cloud Shell. Check the `AllowAllAzureServicesAndResourcesWithinAzureIps` rule exists: `az postgres flexible-server firewall-rule list --resource-group rg-civicgrant-shared --server-name "$server" --output table` |
| `password authentication failed` | Wrong role name or an expired token. Repeat steps 4 and 5, and compare `$admin` with the administrator shown on the server's Authentication page |
| `certificate verify failed` | psql older than 16, or no system certificate store. Use `PGSSLMODE=require` and `unset PGSSLROOTCERT` |
| `you cannot act as the database owner` | Your administrator role isn't a member of the role that owns the database. The script stopped before changing anything; the line before the error names the owner. Don't work around it with broader grants: find out why the owner differs first |
| `verification failed` | Something didn't apply. The transaction was rolled back, so nothing changed |
