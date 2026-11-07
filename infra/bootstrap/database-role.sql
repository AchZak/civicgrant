-- ============================================================
-- BOOTSTRAP: AN ENVIRONMENT'S APP ROLE IN POSTGRESQL
-- ============================================================
--
-- Lets one environment's application sign in to PostgreSQL with its managed
-- identity, and reach its own database only:
--
--   role id-civicgrant-app-<env>   a PostgreSQL role mapped to the identity's
--                                  object id: a token for that identity signs in as it
--   database civicgrant_<env>      only that role (and the server's administrators)
--                                  may connect; the role may create schemas in it
--
-- The application's Flyway creates the civicgrant schema on its first start, so the
-- app owns its schema and everything in it. No other grant is ever needed.
--
-- Run by the server's Entra administrator, by hand, from Azure Cloud Shell, connected
-- to the postgres database: infra/bootstrap/README.md is the runbook. Bicep can't do
-- this, because database roles live inside PostgreSQL, not in Azure Resource Manager.
--
-- Safe to run again: it checks what exists and changes only what differs. The changes
-- run in one transaction, checked before the commit: if anything is off, nothing changes.
--
-- psql variables, passed with -v:
--   app_role        id-civicgrant-app-tst   role name, the same as the identity's name
--   app_object_id   <guid>                  the identity's principal id (its object id
--                                           in Entra), not its client id
--   database        civicgrant_tst          the environment's database


-- ------------------------------------------------------------
-- SETTINGS
-- ------------------------------------------------------------

-- Stop at the first error, whatever the caller passed. Without it, psql reports the
-- error, carries on with the next statement and still exits with success.
\set ON_ERROR_STOP on


-- ------------------------------------------------------------
-- CHECKS
-- Read-only. Each one stops the script before anything changes.
-- A DO block that raises an exception is how a psql script fails on purpose.
-- ------------------------------------------------------------

-- 1. All three variables are set. :{?name} is psql for "is this variable set".
SELECT :{?app_role} AND :{?app_object_id} AND :{?database} AS variables_set \gset
\if :variables_set
\else
  \echo 'Pass -v app_role=... -v app_object_id=... -v database=... (see infra/bootstrap/README.md).'
  DO $$ BEGIN RAISE EXCEPTION 'missing psql variable'; END $$;
\endif

-- 2. The object id is a GUID. It isn't checked against Entra: pass the principal id,
--    not the client id, which is a GUID too but identifies something else.
SELECT :'app_object_id' ~* '^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$' AS object_id_valid \gset
\if :object_id_valid
\else
  DO $$ BEGIN RAISE EXCEPTION 'app_object_id is not a GUID'; END $$;
\endif

-- 3. The database exists. Bicep creates it (shared.bicep), never this script.
SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_database WHERE datname = :'database') AS database_exists \gset
\if :database_exists
\else
  DO $$ BEGIN RAISE EXCEPTION 'database not found: deploy shared.bicep first'; END $$;
\endif

-- 4. You can act as the database's owner. Only the owner (or a member of the owning
--    role) may grant or revoke rights on a database, and PostgreSQL doesn't fail when
--    someone else tries: it prints a warning and changes nothing. So check first.
--    pg_has_role(..., 'USAGE') means "has the owner's privileges", through membership.
SELECT pg_catalog.pg_get_userbyid(datdba) AS database_owner,
       pg_catalog.pg_has_role(current_user, datdba, 'USAGE') AS acts_as_owner
  FROM pg_catalog.pg_database
 WHERE datname = :'database' \gset
\echo 'Signed in as' :USER '; database' :database 'is owned by' :database_owner
\if :acts_as_owner
\else
  DO $$ BEGIN RAISE EXCEPTION 'you cannot act as the database owner: see Troubleshooting in infra/bootstrap/README.md'; END $$;
\endif


-- ------------------------------------------------------------
-- CHANGES
-- One transaction: the checks at the end decide whether it's committed.
-- ------------------------------------------------------------

BEGIN;

-- 1. The app's role, mapped to its managed identity.
--    Azure records the mapping as a security label on the role ("pgaadauth" provider):
--    at sign-in, the token's object id must match the label's oid.
--      - Missing role: Microsoft's function creates it with the label. 'service' is the
--        type for applications and managed identities; false, false = not an admin,
--        no MFA check (an identity has no MFA).
--      - Existing role, labelled with another object id: the identity was deleted and
--        recreated, so it has a new object id. Relabelling keeps the role, and with it
--        the schema it owns and all the data; dropping it would lose both.
--      - Existing role with the right label: nothing to do.
SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'app_role') AS role_exists \gset
\if :role_exists
  SELECT EXISTS (
           SELECT 1
             FROM pg_catalog.pg_seclabels
            WHERE provider = 'pgaadauth'
              AND objtype = 'role'
              AND objoid = (SELECT oid FROM pg_catalog.pg_roles WHERE rolname = :'app_role')
              AND position(lower('oid=' || :'app_object_id') IN lower(label)) > 0
         ) AS role_mapped \gset
  \if :role_mapped
    \echo 'Role' :app_role 'already mapped to' :app_object_id ': unchanged.'
  \else
    \echo 'Role' :app_role 'exists with another object id: mapping it to' :app_object_id
    \set entra_label 'aadauth,oid=' :app_object_id ',type=service'
    SECURITY LABEL FOR "pgaadauth" ON ROLE :"app_role" IS :'entra_label';
  \endif
\else
  \echo 'Creating role' :app_role 'for object id' :app_object_id
  SELECT pg_catalog.pgaadauth_create_principal_with_oid(:'app_role', :'app_object_id', 'service', false, false);
\endif

-- 2. Who may use the database.
--    PostgreSQL lets every role (PUBLIC) connect to every database by default, so the
--    other environments' app roles could reach this one. Revoke that, then let in this
--    environment's role only. The administrators keep access through the owner role.
--    CONNECT: sign in to this database. CREATE: create schemas in it, so Flyway can
--    create the civicgrant schema and the app owns it. No TEMPORARY: nothing uses
--    temporary tables. Both statements change nothing when already applied.
REVOKE ALL ON DATABASE :"database" FROM PUBLIC;
GRANT CONNECT, CREATE ON DATABASE :"database" TO :"app_role";


-- ------------------------------------------------------------
-- VERIFICATION
-- Before committing, read back what the database now says.
-- has_database_privilege with several privileges is true if ANY is held, so CONNECT
-- and CREATE are checked separately. 'public' is how these functions name PUBLIC.
-- ------------------------------------------------------------

SELECT EXISTS (
         SELECT 1
           FROM pg_catalog.pg_seclabels
          WHERE provider = 'pgaadauth'
            AND objtype = 'role'
            AND objoid = (SELECT oid FROM pg_catalog.pg_roles WHERE rolname = :'app_role')
            AND position(lower('oid=' || :'app_object_id') IN lower(label)) > 0
       )
       AND pg_catalog.has_database_privilege(:'app_role', :'database', 'CONNECT')
       AND pg_catalog.has_database_privilege(:'app_role', :'database', 'CREATE')
       AND NOT pg_catalog.has_database_privilege('public', :'database', 'CONNECT')
       AS verified \gset

\if :verified
  COMMIT;
  \echo 'Done:' :app_role 'may sign in with its identity and use' :database ', and no other role may connect to it.'
\else
  \echo 'Verification failed: rolling back. Nothing was changed.'
  DO $$ BEGIN RAISE EXCEPTION 'verification failed'; END $$;
\endif
