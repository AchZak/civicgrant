package be.achzak.civicgrant.core.grantapplication.domain;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_ASSESSED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_DRAFT;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.APPLICATION_NOT_SUBMITTED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.ASSESSMENT_NOT_FOUND;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.CONFLICT_OF_INTEREST;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.DOCUMENT_NOT_FOUND;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.FUNDING_CALL_CLOSED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.INELIGIBLE_APPLICATION_CANNOT_BE_APPROVED;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.NOT_THE_APPLICANT;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.REQUESTED_AMOUNT_EXCEEDS_CAP;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.SEPARATION_OF_DUTIES;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.SUBMISSION_AFTER_CLOSING_DATE;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.SUBMISSION_BEFORE_OPENING_DATE;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.SUPPORTING_DOCUMENT_REQUIRED;

import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.error.BusinessRuleViolationException;
import be.achzak.civicgrant.core.common.domain.error.ConflictException;
import be.achzak.civicgrant.core.common.domain.error.ErrorCode;
import be.achzak.civicgrant.core.common.domain.error.ForbiddenException;
import be.achzak.civicgrant.core.common.domain.error.NotFoundException;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.UserId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationApproved;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationCreated;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationEdited;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationRejected;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationSubmitted;
import be.achzak.civicgrant.core.grantapplication.domain.event.AssessmentRecorded;
import be.achzak.civicgrant.core.grantapplication.domain.event.DocumentAttached;
import be.achzak.civicgrant.core.grantapplication.domain.event.DocumentRemoved;
import be.achzak.civicgrant.core.grantapplication.domain.event.GrantApplicationEvent;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class GrantApplication {

    private final ApplicationId id;
    private final ApplicationReference reference;
    private final FundingCallId fundingCallId;
    private final OrganisationId organisationId;
    private final UserId applicant;
    private ProjectProposal proposal;
    private final List<SupportingDocument> documents;
    private ApplicationStatus status;
    private @Nullable Instant submittedAt;
    private @Nullable Assessment assessment;
    private @Nullable Decision decision;
    private final Instant createdAt;
    private Instant updatedAt;
    private final long version;
    private final List<GrantApplicationEvent> events = new ArrayList<>();

    private GrantApplication(GrantApplicationSnapshot snapshot) {
        this.id = snapshot.id();
        this.reference = snapshot.reference();
        this.fundingCallId = snapshot.fundingCallId();
        this.organisationId = snapshot.organisationId();
        this.applicant = snapshot.applicant();
        this.proposal = snapshot.proposal();
        this.documents = new ArrayList<>(snapshot.documents());
        this.status = snapshot.status();
        this.submittedAt = snapshot.submittedAt();
        this.assessment = snapshot.assessment();
        this.decision = snapshot.decision();
        this.createdAt = snapshot.createdAt();
        this.updatedAt = snapshot.updatedAt();
        this.version = snapshot.version();
    }

    public static GrantApplication create(
            ApplicationReference reference,
            FundingCall fundingCall,
            Organisation organisation,
            ProjectProposal proposal,
            EligibilityPolicy eligibilityPolicy,
            Actor applicant,
            Instant now) {
        ensureAcceptsDrafts(fundingCall, now);
        eligibilityPolicy.ensureEligible(organisation);
        ensureWithinCap(proposal, fundingCall);
        GrantApplication application = new GrantApplication(new GrantApplicationSnapshot(
                ApplicationId.generate(),
                reference,
                fundingCall.id(),
                organisation.id(),
                applicant.id(),
                proposal,
                List.of(),
                ApplicationStatus.DRAFT,
                null,
                null,
                null,
                now,
                now,
                0));
        application.events.add(
                new ApplicationCreated(application.id, fundingCall.id(), organisation.id(), applicant.id(), now));
        return application;
    }

    public static GrantApplication restore(GrantApplicationSnapshot snapshot) {
        return new GrantApplication(snapshot);
    }

    public void edit(ProjectProposal newProposal, FundingCall fundingCall, Actor actor, Instant now) {
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, APPLICATION_NOT_DRAFT);
        ensureSameCall(fundingCall);
        ensureAcceptsDrafts(fundingCall, now);
        ensureWithinCap(newProposal, fundingCall);
        proposal = newProposal;
        updatedAt = now;
        events.add(new ApplicationEdited(id, newProposal.requestedAmount(), actor.id(), now));
    }

    public SupportingDocument attachDocument(
            String fileName,
            String declaredContentType,
            byte[] content,
            FundingCall fundingCall,
            Actor actor,
            Instant now) {
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, APPLICATION_NOT_DRAFT);
        ensureSameCall(fundingCall);
        ensureAcceptsDrafts(fundingCall, now);
        SupportingDocument document =
                SupportingDocument.upload(fileName, declaredContentType, content, actor.id(), now);
        documents.add(document);
        updatedAt = now;
        events.add(new DocumentAttached(id, document.id(), actor.id(), now));
        return document;
    }

    public void removeDocument(DocumentId documentId, Actor actor, Instant now) {
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, APPLICATION_NOT_DRAFT);
        if (!documents.removeIf(document -> document.id().equals(documentId))) {
            throw documentNotFound(documentId);
        }
        updatedAt = now;
        events.add(new DocumentRemoved(id, documentId, actor.id(), now));
    }

    public SupportingDocument document(DocumentId documentId, Actor reader) {
        ensureVisibleTo(reader);
        return documents.stream()
                .filter(document -> document.id().equals(documentId))
                .findFirst()
                .orElseThrow(() -> documentNotFound(documentId));
    }

    public Assessment assessmentFor(Actor reader) {
        ensureVisibleTo(reader);
        ensureNotTheApplicant(reader);
        if (assessment == null) {
            throw new NotFoundException(
                    ASSESSMENT_NOT_FOUND, "Application " + reference.value() + " hasn't been assessed");
        }
        return assessment;
    }

    private NotFoundException documentNotFound(DocumentId documentId) {
        return new NotFoundException(
                DOCUMENT_NOT_FOUND,
                "Document " + documentId.value() + " not found in application " + reference.value());
    }

    public void submit(FundingCall fundingCall, Actor actor, Instant now) {
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, APPLICATION_NOT_DRAFT);
        ensureSameCall(fundingCall);
        SubmissionPeriod period = fundingCall.submissionPeriod();
        if (period.isUpcomingAt(now)) {
            throw new BusinessRuleViolationException(
                    SUBMISSION_BEFORE_OPENING_DATE,
                    "Funding call " + fundingCall.reference().value() + " opens at " + period.opensAt());
        }
        if (period.isClosedAt(now)) {
            throw new BusinessRuleViolationException(
                    SUBMISSION_AFTER_CLOSING_DATE,
                    "Funding call " + fundingCall.reference().value() + " closed at " + period.closesAt());
        }
        if (documents.isEmpty()) {
            throw new BusinessRuleViolationException(
                    SUPPORTING_DOCUMENT_REQUIRED,
                    "Application " + reference.value() + " needs at least one supporting document");
        }
        ensureWithinCap(proposal, fundingCall);
        status = ApplicationStatus.SUBMITTED;
        submittedAt = now;
        updatedAt = now;
        events.add(new ApplicationSubmitted(id, proposal.requestedAmount(), actor.id(), now));
    }

    public void recordAssessment(
            boolean eligible, Recommendation recommendation, String comment, Actor assessor, Instant now) {
        ensureVisibleTo(assessor);
        ensureNotTheApplicant(assessor);
        ensureStatus(ApplicationStatus.SUBMITTED, APPLICATION_NOT_SUBMITTED);
        assessment =
                Assessment.record(assessor.id(), eligible, recommendation, comment, proposal.requestedAmount(), now);
        status = ApplicationStatus.ASSESSED;
        updatedAt = now;
        events.add(new AssessmentRecorded(id, eligible, recommendation, assessor.id(), now));
    }

    public void approve(Money grantedAmount, String motivation, Actor decisionMaker, Instant now) {
        Assessment recorded = ensureDecidableBy(decisionMaker);
        if (!recorded.eligible()) {
            throw new BusinessRuleViolationException(
                    INELIGIBLE_APPLICATION_CANNOT_BE_APPROVED,
                    "Application " + reference.value() + " was assessed as ineligible");
        }
        decision = Decision.approval(grantedAmount, proposal.requestedAmount(), decisionMaker.id(), motivation, now);
        status = ApplicationStatus.APPROVED;
        updatedAt = now;
        events.add(new ApplicationApproved(id, grantedAmount, decisionMaker.id(), now));
    }

    public void reject(RejectionGround ground, String motivation, Actor decisionMaker, Instant now) {
        ensureDecidableBy(decisionMaker);
        decision = Decision.rejection(ground, decisionMaker.id(), motivation, now);
        status = ApplicationStatus.REJECTED;
        updatedAt = now;
        events.add(new ApplicationRejected(id, ground, decisionMaker.id(), now));
    }

    private Assessment ensureDecidableBy(Actor decisionMaker) {
        ensureVisibleTo(decisionMaker);
        ensureNotTheApplicant(decisionMaker);
        ensureStatus(ApplicationStatus.ASSESSED, APPLICATION_NOT_ASSESSED);
        Assessment recorded = Objects.requireNonNull(assessment, "an assessed application has its assessment");
        if (recorded.assessor().equals(decisionMaker.id())) {
            throw new ForbiddenException(
                    SEPARATION_OF_DUTIES, "The assessor of application " + reference.value() + " can't decide on it");
        }
        return recorded;
    }

    public boolean isVisibleTo(Actor actor) {
        return ApplicationVisibility.of(actor).allows(applicant, status);
    }

    private void ensureActingApplicant(Actor actor) {
        if (applicant.equals(actor.id())) {
            return;
        }
        if (isVisibleTo(actor)) {
            throw new ForbiddenException(
                    NOT_THE_APPLICANT, "Only the applicant may change application " + reference.value());
        }
        throw notFound();
    }

    private void ensureVisibleTo(Actor actor) {
        if (!isVisibleTo(actor)) {
            throw notFound();
        }
    }

    private void ensureNotTheApplicant(Actor actor) {
        if (applicant.equals(actor.id())) {
            throw new ForbiddenException(
                    CONFLICT_OF_INTEREST,
                    "The applicant of application " + reference.value() + " can't assess or decide on it");
        }
    }

    private NotFoundException notFound() {
        return new NotFoundException(APPLICATION_NOT_FOUND, "Application " + id.value() + " not found");
    }

    private void ensureStatus(ApplicationStatus expected, ErrorCode code) {
        if (status != expected) {
            throw new ConflictException(
                    code, "Application " + reference.value() + " is " + status + ", not " + expected);
        }
    }

    private void ensureSameCall(FundingCall fundingCall) {
        if (!fundingCallId.equals(fundingCall.id())) {
            throw new IllegalArgumentException(
                    "Application " + reference.value() + " belongs to another funding call than " + fundingCall.id());
        }
    }

    private static void ensureAcceptsDrafts(FundingCall fundingCall, Instant now) {
        if (fundingCall.submissionPeriod().isClosedAt(now)) {
            throw new BusinessRuleViolationException(
                    FUNDING_CALL_CLOSED,
                    "Funding call " + fundingCall.reference().value() + " closed at "
                            + fundingCall.submissionPeriod().closesAt());
        }
    }

    private static void ensureWithinCap(ProjectProposal proposal, FundingCall fundingCall) {
        if (proposal.requestedAmount().isGreaterThan(fundingCall.capPerApplication())) {
            throw new BusinessRuleViolationException(
                    REQUESTED_AMOUNT_EXCEEDS_CAP,
                    "Requested amount " + proposal.requestedAmount().amount() + " exceeds the cap of "
                            + fundingCall.capPerApplication().amount() + " in call "
                            + fundingCall.reference().value());
        }
    }

    public List<GrantApplicationEvent> pullEvents() {
        List<GrantApplicationEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public ApplicationId id() {
        return id;
    }

    public ApplicationReference reference() {
        return reference;
    }

    public FundingCallId fundingCallId() {
        return fundingCallId;
    }

    public OrganisationId organisationId() {
        return organisationId;
    }

    public UserId applicant() {
        return applicant;
    }

    public ProjectProposal proposal() {
        return proposal;
    }

    public List<SupportingDocument> documents() {
        return List.copyOf(documents);
    }

    public ApplicationStatus status() {
        return status;
    }

    public Optional<Instant> submittedAt() {
        return Optional.ofNullable(submittedAt);
    }

    public Optional<Assessment> assessment() {
        return Optional.ofNullable(assessment);
    }

    public Optional<Decision> decision() {
        return Optional.ofNullable(decision);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
