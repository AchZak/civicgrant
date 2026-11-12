package be.achzak.civicgrant.domain.audit.port.in;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;
import java.util.Objects;

public record RecordAuditEntryCommand(AuditEntry entry) {

    public RecordAuditEntryCommand {
        Objects.requireNonNull(entry, "entry");
    }
}
