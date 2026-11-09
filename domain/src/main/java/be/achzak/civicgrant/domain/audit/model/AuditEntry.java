package be.achzak.civicgrant.domain.audit.model;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationApproved;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationCreated;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationEdited;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationRejected;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSubmitted;
import be.achzak.civicgrant.domain.grantapplication.model.AssessmentRecorded;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentAttached;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentRemoved;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationEvent;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditEntry(
        ApplicationId applicationId,
        AuditAction action,
        UserId actorId,
        Instant occurredAt,
        Map<String, String> details) {

    public AuditEntry {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        details = Map.copyOf(Objects.requireNonNull(details, "details"));
    }

    public static AuditEntry from(GrantApplicationEvent event) {
        Objects.requireNonNull(event, "event");
        return switch (event) {
            case ApplicationCreated created ->
                entry(
                        created,
                        AuditAction.APPLICATION_CREATED,
                        Map.of(
                                "fundingCallId", created.fundingCallId().value().toString(),
                                "organisationId",
                                        created.organisationId().value().toString()));
            case ApplicationEdited edited ->
                entry(
                        edited,
                        AuditAction.APPLICATION_EDITED,
                        Map.of("requestedAmount", amount(edited.requestedAmount())));
            case DocumentAttached attached ->
                entry(
                        attached,
                        AuditAction.DOCUMENT_ATTACHED,
                        Map.of("documentId", attached.documentId().value().toString()));
            case DocumentRemoved removed ->
                entry(
                        removed,
                        AuditAction.DOCUMENT_REMOVED,
                        Map.of("documentId", removed.documentId().value().toString()));
            case ApplicationSubmitted submitted ->
                entry(
                        submitted,
                        AuditAction.APPLICATION_SUBMITTED,
                        Map.of("requestedAmount", amount(submitted.requestedAmount())));
            case AssessmentRecorded assessed ->
                entry(assessed, AuditAction.ASSESSMENT_RECORDED, assessmentDetails(assessed));
            case ApplicationApproved approved ->
                entry(
                        approved,
                        AuditAction.APPLICATION_APPROVED,
                        Map.of("grantedAmount", amount(approved.grantedAmount())));
            case ApplicationRejected rejected ->
                entry(
                        rejected,
                        AuditAction.APPLICATION_REJECTED,
                        Map.of("rejectionGround", rejected.ground().name()));
        };
    }

    private static Map<String, String> assessmentDetails(AssessmentRecorded assessed) {
        String eligible = String.valueOf(assessed.eligible());
        return switch (assessed.recommendation()) {
            case Recommendation.Approve(Money proposedAmount) ->
                Map.of("eligible", eligible, "recommendation", "APPROVE", "proposedAmount", amount(proposedAmount));
            case Recommendation.Reject _ -> Map.of("eligible", eligible, "recommendation", "REJECT");
        };
    }

    private static AuditEntry entry(GrantApplicationEvent event, AuditAction action, Map<String, String> details) {
        return new AuditEntry(event.applicationId(), action, event.actorId(), event.occurredAt(), details);
    }

    private static String amount(Money money) {
        return money.amount().toPlainString();
    }
}
