package be.achzak.civicgrant;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.domain.audit.port.in.RecordAuditEntryUseCase;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationEvent;
import be.achzak.civicgrant.domain.organisation.model.OrganisationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AuditTrailListener {

    private final RecordAuditEntryUseCase recordAuditEntry;

    AuditTrailListener(RecordAuditEntryUseCase recordAuditEntry) {
        this.recordAuditEntry = recordAuditEntry;
    }

    @EventListener
    void on(OrganisationEvent event) {
        recordAuditEntry.record(new RecordAuditEntryCommand(AuditEntry.from(event)));
    }

    @EventListener
    void on(GrantApplicationEvent event) {
        recordAuditEntry.record(new RecordAuditEntryCommand(AuditEntry.from(event)));
    }
}
