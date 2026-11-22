package be.achzak.civicgrant.starter.event;

import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryUseCase;
import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallEvent;
import be.achzak.civicgrant.core.grantapplication.domain.event.GrantApplicationEvent;
import be.achzak.civicgrant.core.organisation.domain.event.OrganisationEvent;
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

    @EventListener
    void on(FundingCallEvent event) {
        AuditEntry.from(event).ifPresent(entry -> recordAuditEntry.record(new RecordAuditEntryCommand(entry)));
    }
}
