package be.achzak.civicgrant.core.audit.application.port.out;

import be.achzak.civicgrant.core.audit.domain.AuditEntry;

public interface AuditTrail {

    void append(AuditEntry entry);
}
