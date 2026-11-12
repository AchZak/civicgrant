package be.achzak.civicgrant.domain.audit.service;

import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("appends the entries to the audit trail in the order they are recorded")
    void appendsTheEntriesInTheOrderTheyAreRecorded() {
        // given
        InMemoryAuditTrail auditTrail = new InMemoryAuditTrail();
        RecordAuditEntryService service = new RecordAuditEntryService(auditTrail);
        ApplicationId application = ApplicationId.generate();
        AuditEntry edited = AuditEntry.from(new ApplicationEdited(
                application, euros("9000.00"), anApplicant().id(), NOW));
        AuditEntry submitted = AuditEntry.from(new ApplicationSubmitted(
                application, euros("9000.00"), anApplicant().id(), NOW.plusSeconds(60)));

        // when
        service.record(new RecordAuditEntryCommand(edited));
        service.record(new RecordAuditEntryCommand(submitted));

        // then
        assertThat(auditTrail.entries()).containsExactly(edited, submitted);
    }
}
