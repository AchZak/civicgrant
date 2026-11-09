package be.achzak.civicgrant.domain.audit.model;

import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationApproved;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationCreated;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationEdited;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationRejected;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSubmitted;
import be.achzak.civicgrant.domain.grantapplication.model.AssessmentRecorded;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentAttached;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentRemoved;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Audit entry")
class AuditEntryTest {

    private static final ApplicationId APPLICATION =
            new ApplicationId(UUID.fromString("0f6e1d1c-0001-4000-8000-000000000001"));
    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

    @Test
    @DisplayName("keeps the application, the actor and the moment of the event")
    void keepsTheApplicationTheActorAndTheMoment() {
        // given
        ApplicationSubmitted submitted = new ApplicationSubmitted(
                APPLICATION, euros("8000.00"), anApplicant().id(), NOW);

        // when
        AuditEntry entry = AuditEntry.from(submitted);

        // then
        assertThat(entry.applicationId()).isEqualTo(APPLICATION);
        assertThat(entry.actorId()).isEqualTo(anApplicant().id());
        assertThat(entry.occurredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("records a creation with its funding call and organisation")
    void recordsCreation() {
        // given
        FundingCallId call = new FundingCallId(UUID.fromString("0f6e1d1c-0002-4000-8000-000000000002"));
        OrganisationId organisation = new OrganisationId(UUID.fromString("0f6e1d1c-0003-4000-8000-000000000003"));

        // when
        AuditEntry entry = AuditEntry.from(new ApplicationCreated(
                APPLICATION, call, organisation, anApplicant().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.APPLICATION_CREATED);
        assertThat(entry.details())
                .isEqualTo(Map.of(
                        "fundingCallId", "0f6e1d1c-0002-4000-8000-000000000002",
                        "organisationId", "0f6e1d1c-0003-4000-8000-000000000003"));
    }

    @Test
    @DisplayName("records an edit with the new requested amount")
    void recordsEdit() {
        // when
        AuditEntry entry = AuditEntry.from(new ApplicationEdited(
                APPLICATION, euros("9000.00"), anApplicant().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.APPLICATION_EDITED);
        assertThat(entry.details()).isEqualTo(Map.of("requestedAmount", "9000.00"));
    }

    @Test
    @DisplayName("records an attached document by its id only")
    void recordsAttachedDocument() {
        // given
        DocumentId document = new DocumentId(UUID.fromString("0f6e1d1c-0004-4000-8000-000000000004"));

        // when
        AuditEntry entry = AuditEntry.from(
                new DocumentAttached(APPLICATION, document, anApplicant().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.DOCUMENT_ATTACHED);
        assertThat(entry.details()).isEqualTo(Map.of("documentId", "0f6e1d1c-0004-4000-8000-000000000004"));
    }

    @Test
    @DisplayName("records a removed document by its id only")
    void recordsRemovedDocument() {
        // given
        DocumentId document = new DocumentId(UUID.fromString("0f6e1d1c-0004-4000-8000-000000000004"));

        // when
        AuditEntry entry = AuditEntry.from(
                new DocumentRemoved(APPLICATION, document, anApplicant().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.DOCUMENT_REMOVED);
        assertThat(entry.details()).isEqualTo(Map.of("documentId", "0f6e1d1c-0004-4000-8000-000000000004"));
    }

    @Test
    @DisplayName("records a submission with the requested amount")
    void recordsSubmission() {
        // when
        AuditEntry entry = AuditEntry.from(new ApplicationSubmitted(
                APPLICATION, euros("8000.00"), anApplicant().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.APPLICATION_SUBMITTED);
        assertThat(entry.details()).isEqualTo(Map.of("requestedAmount", "8000.00"));
    }

    @Test
    @DisplayName("records an approval recommendation with the proposed amount")
    void recordsApprovalRecommendation() {
        // given
        Recommendation approve = new Recommendation.Approve(euros("7500.00"));

        // when
        AuditEntry entry = AuditEntry.from(
                new AssessmentRecorded(APPLICATION, true, approve, anAssessor().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.ASSESSMENT_RECORDED);
        assertThat(entry.details())
                .isEqualTo(Map.of("eligible", "true", "recommendation", "APPROVE", "proposedAmount", "7500.00"));
    }

    @Test
    @DisplayName("records a rejection recommendation without an amount")
    void recordsRejectionRecommendation() {
        // given
        Recommendation reject = new Recommendation.Reject();

        // when
        AuditEntry entry = AuditEntry.from(
                new AssessmentRecorded(APPLICATION, false, reject, anAssessor().id(), NOW));

        // then
        assertThat(entry.details()).isEqualTo(Map.of("eligible", "false", "recommendation", "REJECT"));
    }

    @Test
    @DisplayName("records an approval with the granted amount, not the motivation")
    void recordsApproval() {
        // when
        AuditEntry entry = AuditEntry.from(new ApplicationApproved(
                APPLICATION, euros("7000.00"), aDecisionMaker().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.APPLICATION_APPROVED);
        assertThat(entry.details()).isEqualTo(Map.of("grantedAmount", "7000.00"));
    }

    @Test
    @DisplayName("records a rejection with its ground, not the motivation")
    void recordsRejection() {
        // when
        AuditEntry entry = AuditEntry.from(new ApplicationRejected(
                APPLICATION, RejectionGround.INELIGIBLE, aDecisionMaker().id(), NOW));

        // then
        assertThat(entry.action()).isEqualTo(AuditAction.APPLICATION_REJECTED);
        assertThat(entry.details()).isEqualTo(Map.of("rejectionGround", "INELIGIBLE"));
    }
}
