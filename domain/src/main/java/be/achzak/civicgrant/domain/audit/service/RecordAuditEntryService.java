package be.achzak.civicgrant.domain.audit.service;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import be.achzak.civicgrant.domain.audit.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.domain.audit.port.in.RecordAuditEntryUseCase;
import be.achzak.civicgrant.domain.audit.port.out.AuditTrail;
import java.util.Objects;

public class RecordAuditEntryService implements RecordAuditEntryUseCase {

    private final AuditTrail auditTrail;

    public RecordAuditEntryService(AuditTrail auditTrail) {
        this.auditTrail = Objects.requireNonNull(auditTrail, "auditTrail");
    }

    @Override
    public void record(RecordAuditEntryCommand command) {
        Objects.requireNonNull(command, "command");
        auditTrail.append(AuditEntry.from(command.event()));
    }
}
