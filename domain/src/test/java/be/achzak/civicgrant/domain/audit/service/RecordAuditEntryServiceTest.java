package be.achzak.civicgrant.domain.audit.service;

import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.audit.model.AuditAction;
import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.domain.audit.port.out.InMemoryAuditTrail;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationEdited;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSubmitted;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Recording an audit entry")
class RecordAuditEntryServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    @Test
    @DisplayName("appends the entry made from the event to the audit trail")
    void appendsTheEntryMadeFromTheEvent() {
        // given
        InMemoryAuditTrail auditTrail = new InMemoryAuditTrail();
        RecordAuditEntryService service = new RecordAuditEntryService(auditTrail);
        ApplicationSubmitted submitted = new ApplicationSubmitted(
                ApplicationId.generate(), euros("8000.00"), anApplicant().id(), NOW);

        // when
        service.record(new RecordAuditEntryCommand(submitted));

        // then
        assertThat(auditTrail.entries()).containsExactly(AuditEntry.from(submitted));
    }

    @Test
    @DisplayName("appends one entry per event, in order")
    void appendsOneEntryPerEventInOrder() {
        // given
        InMemoryAuditTrail auditTrail = new InMemoryAuditTrail();
        RecordAuditEntryService service = new RecordAuditEntryService(auditTrail);
        ApplicationId application = ApplicationId.generate();

        // when
        service.record(new RecordAuditEntryCommand(new ApplicationEdited(
                application, euros("9000.00"), anApplicant().id(), NOW)));
        service.record(new RecordAuditEntryCommand(new ApplicationSubmitted(
                application, euros("9000.00"), anApplicant().id(), NOW.plusSeconds(60))));

        // then
        assertThat(auditTrail.entries())
                .extracting(AuditEntry::action)
                .containsExactly(AuditAction.APPLICATION_EDITED, AuditAction.APPLICATION_SUBMITTED);
    }
}
