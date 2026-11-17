-- An application has at most one assessment, so the application's id is the assessment's key.
ALTER TABLE assessment
    DROP CONSTRAINT uq_assessment_application,
    DROP COLUMN id,
    ADD PRIMARY KEY (grant_application_id),
    ADD CONSTRAINT ck_assessment_comment CHECK (LENGTH(TRIM(comment)) > 0);
