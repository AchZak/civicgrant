package be.achzak.civicgrant.core.audit.application.port.in;

import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.sharedkernel.application.Command;

public record RecordAuditEntryCommand(AuditEntry entry) implements Command {}
