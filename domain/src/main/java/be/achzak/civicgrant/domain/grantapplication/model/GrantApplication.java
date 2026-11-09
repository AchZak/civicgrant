package be.achzak.civicgrant.domain.grantapplication.model;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.ForbiddenException;
import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GrantApplication {

    private final ApplicationId id;
    private final ApplicationReference reference;
    private final FundingCallId fundingCallId;
    private final OrganisationId organisationId;
    private final UserId applicant;
    private ProjectProposal proposal;
    private final List<SupportingDocument> documents;
    private ApplicationStatus status;
    private Instant submittedAt;
    private Assessment assessment;
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
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(fundingCall, "fundingCall");
        Objects.requireNonNull(organisation, "organisation");
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy");
        Objects.requireNonNull(applicant, "applicant");
        Objects.requireNonNull(now, "now");
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
                now,
                now,
                0));
        application.events.add(
                new ApplicationCreated(application.id, fundingCall.id(), organisation.id(), applicant.id(), now));
        return application;
    }

    public static GrantApplication restore(GrantApplicationSnapshot snapshot) {
        return new GrantApplication(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public void edit(ProjectProposal newProposal, FundingCall fundingCall, Actor actor, Instant now) {
        Objects.requireNonNull(newProposal, "newProposal");
        Objects.requireNonNull(fundingCall, "fundingCall");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(now, "now");
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, "APPLICATION_NOT_DRAFT");
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
            long sizeInBytes,
            FundingCall fundingCall,
            Actor actor,
            Instant now) {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(declaredContentType, "declaredContentType");
        Objects.requireNonNull(fundingCall, "fundingCall");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(now, "now");
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, "APPLICATION_NOT_DRAFT");
        ensureSameCall(fundingCall);
        ensureAcceptsDrafts(fundingCall, now);
        SupportingDocument document =
                SupportingDocument.upload(fileName, declaredContentType, sizeInBytes, actor.id(), now);
        documents.add(document);
        updatedAt = now;
        events.add(new DocumentAttached(id, document.id(), actor.id(), now));
        return document;
    }

    public void removeDocument(DocumentId documentId, Actor actor, Instant now) {
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(now, "now");
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, "APPLICATION_NOT_DRAFT");
        if (!documents.removeIf(document -> document.id().equals(documentId))) {
            throw new NotFoundException(
                    "DOCUMENT_NOT_FOUND",
                    "Document " + documentId.value() + " not found in application " + reference.value());
        }
        updatedAt = now;
        events.add(new DocumentRemoved(id, documentId, actor.id(), now));
    }

    public void submit(FundingCall fundingCall, Actor actor, Instant now) {
        Objects.requireNonNull(fundingCall, "fundingCall");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(now, "now");
        ensureActingApplicant(actor);
        ensureStatus(ApplicationStatus.DRAFT, "APPLICATION_NOT_DRAFT");
        ensureSameCall(fundingCall);
        SubmissionPeriod period = fundingCall.submissionPeriod();
        if (period.isUpcomingAt(now)) {
            throw new BusinessRuleViolationException(
                    "SUBMISSION_BEFORE_OPENING_DATE",
                    "Funding call " + fundingCall.reference() + " opens at " + period.opensAt());
        }
        if (period.isClosedAt(now)) {
            throw new BusinessRuleViolationException(
                    "SUBMISSION_AFTER_CLOSING_DATE",
                    "Funding call " + fundingCall.reference() + " closed at " + period.closesAt());
        }
        if (documents.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "SUPPORTING_DOCUMENT_REQUIRED",
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
        Objects.requireNonNull(recommendation, "recommendation");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(assessor, "assessor");
        Objects.requireNonNull(now, "now");
        ensureVisibleTo(assessor);
        ensureNotTheApplicant(assessor);
        ensureStatus(ApplicationStatus.SUBMITTED, "APPLICATION_NOT_SUBMITTED");
        String validComment = RequiredText.strip(
                comment, Assessment.MAX_COMMENT_LENGTH, "ASSESSMENT_COMMENT_REQUIRED", "ASSESSMENT_COMMENT_TOO_LONG");
        if (recommendation instanceof Recommendation.Approve(Money proposedAmount)) {
            ensureApprovalCanBeRecommended(eligible, proposedAmount);
        }
        assessment = new Assessment(assessor.id(), eligible, recommendation, validComment, now);
        status = ApplicationStatus.ASSESSED;
        updatedAt = now;
        events.add(new AssessmentRecorded(id, eligible, recommendation, assessor.id(), now));
    }

    private void ensureApprovalCanBeRecommended(boolean eligible, Money proposedAmount) {
        if (!eligible) {
            throw new BusinessRuleViolationException(
                    "APPROVAL_RECOMMENDATION_REQUIRES_ELIGIBILITY",
                    "Application " + reference.value() + " can't be recommended for approval when ineligible");
        }
        if (!proposedAmount.isPositive()) {
            throw new BusinessRuleViolationException(
                    "PROPOSED_AMOUNT_NOT_POSITIVE", "A proposed amount is greater than zero");
        }
        if (proposedAmount.isGreaterThan(proposal.requestedAmount())) {
            throw new BusinessRuleViolationException(
                    "PROPOSED_AMOUNT_EXCEEDS_REQUESTED",
                    "Proposed amount " + proposedAmount.amount() + " exceeds the requested amount of "
                            + proposal.requestedAmount().amount());
        }
    }

    public boolean isVisibleTo(Actor actor) {
        Objects.requireNonNull(actor, "actor");
        boolean staff = actor.hasRole(Role.ASSESSOR) || actor.hasRole(Role.DECISION_MAKER);
        return applicant.equals(actor.id()) || (staff && status != ApplicationStatus.DRAFT);
    }

    private void ensureActingApplicant(Actor actor) {
        if (applicant.equals(actor.id())) {
            return;
        }
        if (isVisibleTo(actor)) {
            throw new ForbiddenException(
                    "NOT_THE_APPLICANT", "Only the applicant may change application " + reference.value());
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
                    "CONFLICT_OF_INTEREST",
                    "The applicant of application " + reference.value() + " can't assess or decide on it");
        }
    }

    private NotFoundException notFound() {
        return new NotFoundException("APPLICATION_NOT_FOUND", "Application " + id.value() + " not found");
    }

    private void ensureStatus(ApplicationStatus expected, String code) {
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
                    "FUNDING_CALL_CLOSED",
                    "Funding call " + fundingCall.reference() + " closed at "
                            + fundingCall.submissionPeriod().closesAt());
        }
    }

    private static void ensureWithinCap(ProjectProposal proposal, FundingCall fundingCall) {
        if (proposal.requestedAmount().isGreaterThan(fundingCall.capPerApplication())) {
            throw new BusinessRuleViolationException(
                    "REQUESTED_AMOUNT_EXCEEDS_CAP",
                    "Requested amount " + proposal.requestedAmount().amount() + " exceeds the cap of "
                            + fundingCall.capPerApplication().amount() + " in call " + fundingCall.reference());
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
