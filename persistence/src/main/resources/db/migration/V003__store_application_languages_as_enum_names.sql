-- Store the domain's Language names, as every other enum column does.
ALTER TABLE grant_application
    DROP CONSTRAINT ck_grant_application_language,
    ALTER COLUMN language TYPE VARCHAR(2),
    ADD CONSTRAINT ck_grant_application_language CHECK (language IN ('NL', 'FR'));
