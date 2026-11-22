package be.achzak.civicgrant.core.audit.application.service;

import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryUseCase;
import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import java.util.Objects;

public class RecordAuditEntryService implements RecordAuditEntryUseCase {

    private final AuditTrail auditTrail;

    public RecordAuditEntryService(AuditTrail auditTrail) {
        this.auditTrail = Objects.requireNonNull(auditTrail, "auditTrail");
    }

    @Override
    public void record(RecordAuditEntryCommand command) {
        Objects.requireNonNull(command, "command");
        auditTrail.append(command.entry());
    }
}
