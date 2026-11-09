package be.achzak.civicgrant.domain.audit.port.in;

import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationEvent;
import java.util.Objects;

public record RecordAuditEntryCommand(GrantApplicationEvent event) {

    public RecordAuditEntryCommand {
        Objects.requireNonNull(event, "event");
    }
}
