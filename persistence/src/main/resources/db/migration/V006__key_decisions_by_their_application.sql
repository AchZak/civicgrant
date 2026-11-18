-- An application has at most one decision, so the application's id is the decision's key.
ALTER TABLE decision
    DROP CONSTRAINT uq_decision_application,
    DROP COLUMN id,
    ADD PRIMARY KEY (grant_application_id);
