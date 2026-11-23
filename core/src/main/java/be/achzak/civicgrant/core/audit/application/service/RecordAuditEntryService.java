package be.achzak.civicgrant.core.audit.application.service;

import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryCommand;
import be.achzak.civicgrant.core.audit.application.port.in.RecordAuditEntryUseCase;
import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordAuditEntryService implements RecordAuditEntryUseCase {

    private final AuditTrail auditTrail;

    @Override
    @Transactional
    public void record(RecordAuditEntryCommand command) {
        auditTrail.append(command.entry());
    }
}
