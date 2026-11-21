-- An audit event can also be about a funding call, still exactly one subject.
ALTER TABLE audit_event
    ADD COLUMN funding_call_id UUID,
    ADD CONSTRAINT fk_audit_event_funding_call
        FOREIGN KEY (funding_call_id) REFERENCES funding_call (id);

ALTER TABLE audit_event DROP CONSTRAINT ck_audit_event_subject;
ALTER TABLE audit_event
    ADD CONSTRAINT ck_audit_event_subject
        CHECK (num_nonnulls(grant_application_id, organisation_id, funding_call_id) = 1);

ALTER TABLE audit_event DROP CONSTRAINT ck_audit_event_type;
ALTER TABLE audit_event
    ADD CONSTRAINT ck_audit_event_type
        CHECK (event_type IN ('ORGANISATION_REGISTERED', 'APPLICATION_CREATED', 'APPLICATION_EDITED',
                              'DOCUMENT_ATTACHED', 'DOCUMENT_REMOVED', 'APPLICATION_SUBMITTED',
                              'ASSESSMENT_RECORDED', 'APPLICATION_APPROVED', 'APPLICATION_REJECTED',
                              'FUNDING_CALL_OPENED'));

CREATE INDEX ix_audit_event_funding_call ON audit_event (funding_call_id, occurred_at);
