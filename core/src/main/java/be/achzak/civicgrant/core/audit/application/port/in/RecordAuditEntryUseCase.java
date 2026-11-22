package be.achzak.civicgrant.core.audit.application.port.in;

public interface RecordAuditEntryUseCase {

    void record(RecordAuditEntryCommand command);
}
