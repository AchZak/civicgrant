package be.achzak.civicgrant.app.event;

import be.achzak.civicgrant.core.audit.application.port.out.AuditTrail;
import be.achzak.civicgrant.core.audit.domain.AuditEntry;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallEvent;
import be.achzak.civicgrant.core.grantapplication.domain.event.GrantApplicationEvent;
import be.achzak.civicgrant.core.organisation.domain.event.OrganisationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// A plain @EventListener runs inside the use case's transaction, so no change commits without its entry.
@Component
@RequiredArgsConstructor
class AuditTrailListener {

    private final AuditTrail auditTrail;

    @EventListener
    void on(OrganisationEvent event) {
        auditTrail.append(AuditEntry.from(event));
    }

    @EventListener
    void on(GrantApplicationEvent event) {
        auditTrail.append(AuditEntry.from(event));
    }

    @EventListener
    void on(FundingCallEvent event) {
        AuditEntry.from(event).ifPresent(auditTrail::append);
    }
}
