package be.achzak.civicgrant.starter.event;

import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationApproved;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationCreated;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationEdited;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationRejected;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationSubmitted;
import be.achzak.civicgrant.domain.grantapplication.model.event.AssessmentRecorded;
import be.achzak.civicgrant.domain.grantapplication.model.event.DocumentAttached;
import be.achzak.civicgrant.domain.grantapplication.model.event.DocumentRemoved;
import be.achzak.civicgrant.domain.grantapplication.model.event.GrantApplicationEvent;
import be.achzak.civicgrant.domain.shared.Money;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class BusinessMetricsListener {

    static final String APPLICATIONS = "civicgrant.applications";
    static final String GRANTED_AMOUNTS = "civicgrant.grants.amount";

    private final Counter created;
    private final Counter submitted;
    private final Counter assessed;
    private final Counter approved;
    private final Counter rejected;
    private final DistributionSummary grantedAmounts;

    // Registered up front, so every step shows as zero before it first happens.
    BusinessMetricsListener(MeterRegistry registry) {
        created = applications("created", registry);
        submitted = applications("submitted", registry);
        assessed = applications("assessed", registry);
        approved = applications("approved", registry);
        rejected = applications("rejected", registry);
        grantedAmounts = DistributionSummary.builder(GRANTED_AMOUNTS)
                .description("Amounts granted by approved applications")
                .baseUnit("euros")
                .register(registry);
    }

    // Only once committed, so a refused or rolled-back change is never counted.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(GrantApplicationEvent event) {
        switch (event) {
            case ApplicationCreated _ -> created.increment();
            case ApplicationSubmitted _ -> submitted.increment();
            case AssessmentRecorded _ -> assessed.increment();
            case ApplicationApproved approval -> {
                approved.increment();
                grantedAmounts.record(inEuros(approval.grantedAmount()));
            }
            case ApplicationRejected _ -> rejected.increment();
            case ApplicationEdited _, DocumentAttached _, DocumentRemoved _ -> {
                // Not a step of the workflow.
            }
        }
    }

    private static Counter applications(String event, MeterRegistry registry) {
        return Counter.builder(APPLICATIONS)
                .description("Grant applications that reached a step of the workflow")
                .tag("event", event)
                .register(registry);
    }

    // Micrometer only takes doubles. Fine for a statistic, never for the budget itself.
    private static double inEuros(Money amount) {
        return amount.amount().doubleValue();
    }
}
