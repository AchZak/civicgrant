-- An audit event is about exactly one subject: a grant application or an organisation.
ALTER TABLE audit_event
    ADD COLUMN organisation_id UUID,
    ADD CONSTRAINT fk_audit_event_organisation
        FOREIGN KEY (organisation_id) REFERENCES organisation (id),
    ADD CONSTRAINT ck_audit_event_subject
        CHECK (num_nonnulls(grant_application_id, organisation_id) = 1),
    ADD CONSTRAINT ck_audit_event_type
        CHECK (event_type IN ('ORGANISATION_REGISTERED', 'APPLICATION_CREATED', 'APPLICATION_EDITED',
                              'DOCUMENT_ATTACHED', 'DOCUMENT_REMOVED', 'APPLICATION_SUBMITTED',
                              'ASSESSMENT_RECORDED', 'APPLICATION_APPROVED', 'APPLICATION_REJECTED')),
    ALTER COLUMN details SET NOT NULL,
    DROP COLUMN actor_name;

CREATE INDEX ix_audit_event_organisation ON audit_event (organisation_id, occurred_at);
