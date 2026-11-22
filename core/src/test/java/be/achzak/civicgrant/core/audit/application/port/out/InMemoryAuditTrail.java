package be.achzak.civicgrant.core.audit.application.port.out;

import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditTrail implements AuditTrail {

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void append(AuditEntry entry) {
        entries.add(entry);
    }

    public List<AuditEntry> entries() {
        return List.copyOf(entries);
    }
}
