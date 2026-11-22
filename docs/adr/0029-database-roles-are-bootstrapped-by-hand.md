# 29. Database roles are bootstrapped by hand, and each database admits only its own app

Status: accepted

## Context

Bicep can't create PostgreSQL roles, and no database administrator credential exists in CI. By default, PostgreSQL lets every role connect to every database.

## Decision

The server's Entra administrator runs `infra/bootstrap/database-role.sql` once per environment, from Azure Cloud Shell. The script maps a role to the environment's app identity, then takes away PostgreSQL's default "every role may connect" from the environment's database and lets in that role alone. The role may create schemas there.

The script can be rerun safely, and runs as one transaction that reads back its own result before committing, because PostgreSQL only warns when a grant doesn't apply.

## Alternatives considered

Running the script from a developer machine would need a firewall rule for a home IP address, which would stay open for good, since the delete lock blocks removing it. Cloud Shell works because the firewall already admits Azure.

## Consequences

Each database admits only its own app. Flyway creates the `civicgrant` schema, and the app owns everything in it, with no further grants.
