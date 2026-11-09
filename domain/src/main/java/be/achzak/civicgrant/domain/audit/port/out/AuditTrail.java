package be.achzak.civicgrant.domain.audit.port.out;

import be.achzak.civicgrant.domain.audit.model.AuditEntry;

public interface AuditTrail {

    void append(AuditEntry entry);
}
