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
    private ApplicationStatus status;
    private Instant submittedAt;
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
        this.status = snapshot.status();
        this.submittedAt = snapshot.submittedAt();
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
                ApplicationStatus.DRAFT,
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
        ensureWithinCap(proposal, fundingCall);
        status = ApplicationStatus.SUBMITTED;
        submittedAt = now;
        updatedAt = now;
        events.add(new ApplicationSubmitted(id, proposal.requestedAmount(), actor.id(), now));
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
        throw new NotFoundException("APPLICATION_NOT_FOUND", "Application " + id.value() + " not found");
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

    public ApplicationStatus status() {
        return status;
    }

    public Optional<Instant> submittedAt() {
        return Optional.ofNullable(submittedAt);
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
