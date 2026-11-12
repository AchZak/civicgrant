package be.achzak.civicgrant.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.port.out.AuditTrail;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.model.OrganisationRegistered;
import be.achzak.civicgrant.domain.organisation.port.out.OrganisationRepository;
import be.achzak.civicgrant.domain.shared.UserId;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@PersistenceTest
@DisplayName("Audit trail persistence")
class AuditTrailPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0202239951");

    @Autowired
    private AuditTrail auditTrail;

    @Autowired
    private OrganisationRepository organisations;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("stores an entry on its organisation, with the action, actor, moment and details")
    void storesAnEntryOnItsOrganisation() {
        // given
        OrganisationId organisation = OrganisationId.generate();
        organisations.save(
                Organisation.restore(organisation, ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, NOW));
        AuditEntry entry = AuditEntry.from(
                new OrganisationRegistered(organisation, ENTERPRISE_NUMBER, new UserId("applicant-1"), NOW));

        // when
        auditTrail.append(entry);
        entityManager.flush();

        // then
        assertThat(storedEvents())
                .containsExactly(
                        new StoredEvent(null, organisation.value(), "ORGANISATION_REGISTERED", "applicant-1", NOW));
        assertThat(storedDetails())
                .containsExactlyInAnyOrderElementsOf(entry.details().entrySet());
    }

    @Test
    @DisplayName("refuses a row without a subject")
    void refusesARowWithoutASubject() {
        // when
        Throwable thrown = catchThrowable(() -> jdbc.sql("""
                        INSERT INTO civicgrant.audit_event (event_type, actor_oid, occurred_at, details)
                        VALUES ('ORGANISATION_REGISTERED', 'applicant-1', now(), '{}'::jsonb)
                        """).update());

        // then
        assertThat(thrown)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_audit_event_subject");
    }

    private List<StoredEvent> storedEvents() {
        return jdbc.sql("""
                        SELECT grant_application_id, organisation_id, event_type, actor_oid, occurred_at
                        FROM civicgrant.audit_event
                        """)
                .query((row, number) -> new StoredEvent(
                        row.getObject("grant_application_id", UUID.class),
                        row.getObject("organisation_id", UUID.class),
                        row.getString("event_type"),
                        row.getString("actor_oid"),
                        row.getObject("occurred_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    private List<Map.Entry<String, String>> storedDetails() {
        return jdbc.sql("SELECT key, value FROM civicgrant.audit_event, jsonb_each_text(details)")
                .query((row, number) -> Map.entry(row.getString("key"), row.getString("value")))
                .list();
    }

    private record StoredEvent(
            UUID grantApplicationId, UUID organisationId, String eventType, String actorOid, Instant occurredAt) {}
}
