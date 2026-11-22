package be.achzak.civicgrant.core.audit.application.port.in;

import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.sharedkernel.application.Command;
import java.util.Objects;

public record RecordAuditEntryCommand(AuditEntry entry) implements Command {

    public RecordAuditEntryCommand {
        Objects.requireNonNull(entry, "entry");
    }
}
