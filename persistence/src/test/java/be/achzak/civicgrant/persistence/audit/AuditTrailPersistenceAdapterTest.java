package be.achzak.civicgrant.persistence.audit;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.fundingcall.application.port.out.FundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.BudgetEnvelope;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallOpened;
import be.achzak.civicgrant.core.organisation.application.port.out.OrganisationRepository;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.organisation.domain.event.OrganisationRegistered;
import be.achzak.civicgrant.persistence.PersistenceTest;
import java.time.Duration;
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
    private FundingCallRepository fundingCalls;

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
                .containsExactly(new StoredEvent(
                        null, organisation.value(), null, "ORGANISATION_REGISTERED", "applicant-1", NOW));
        assertThat(storedDetails())
                .containsExactlyInAnyOrderElementsOf(entry.details().entrySet());
    }

    @Test
    @DisplayName("stores an entry on its funding call, with the action, actor, moment and details")
    void storesAnEntryOnItsFundingCall() {
        // given
        FundingCallId call = FundingCallId.generate();
        fundingCalls.save(FundingCall.restore(
                call,
                new FundingCallReference("GRN-2027"),
                new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                new SubmissionPeriod(NOW, NOW.plus(Duration.ofDays(90))),
                BudgetEnvelope.unspent(euros("80000.00"), euros("20000.00")),
                0,
                NOW));
        AuditEntry entry = AuditEntry.from(new FundingCallOpened(
                        call,
                        new FundingCallReference("GRN-2027"),
                        euros("80000.00"),
                        new UserId("programme-manager-1"),
                        NOW))
                .orElseThrow();

        // when
        auditTrail.append(entry);
        entityManager.flush();

        // then
        assertThat(storedEvents())
                .containsExactly(
                        new StoredEvent(null, null, call.value(), "FUNDING_CALL_OPENED", "programme-manager-1", NOW));
        assertThat(storedDetails())
                .containsExactlyInAnyOrderElementsOf(entry.details().entrySet());
    }

    @Test
    @DisplayName("refuses a row about both an application and a funding call")
    void refusesARowAboutBothAnApplicationAndAFundingCall() {
        // when
        Throwable thrown = catchThrowable(() -> jdbc.sql("""
                        INSERT INTO civicgrant.audit_event
                            (grant_application_id, funding_call_id, event_type, actor_oid, occurred_at, details)
                        VALUES (:application, :call, 'FUNDING_CALL_OPENED', 'programme-manager-1', now(), '{}'::jsonb)
                        """)
                .param("application", UUID.randomUUID())
                .param("call", UUID.randomUUID())
                .update());

        // then
        assertThat(thrown)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_audit_event_subject");
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
                        SELECT grant_application_id, organisation_id, funding_call_id, event_type, actor_oid,
                               occurred_at
                        FROM civicgrant.audit_event
                        """)
                .query((row, number) -> new StoredEvent(
                        row.getObject("grant_application_id", UUID.class),
                        row.getObject("organisation_id", UUID.class),
                        row.getObject("funding_call_id", UUID.class),
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
            UUID grantApplicationId,
            UUID organisationId,
            UUID fundingCallId,
            String eventType,
            String actorOid,
            Instant occurredAt) {}
}
