package be.achzak.civicgrant.domain.audit.port.in;

public interface RecordAuditEntryUseCase {

    void record(RecordAuditEntryCommand command);
}
