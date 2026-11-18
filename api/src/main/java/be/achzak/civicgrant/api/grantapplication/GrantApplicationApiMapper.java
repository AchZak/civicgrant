package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.model.Application;
import be.achzak.civicgrant.api.generated.model.AssessmentRequest;
import be.achzak.civicgrant.api.generated.model.CreateApplicationRequest;
import be.achzak.civicgrant.api.generated.model.Submission;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.Assessment;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Money;
import io.github.belgif.rest.problem.BadRequestProblem;
import io.github.belgif.rest.problem.api.InEnum;
import io.github.belgif.rest.problem.api.InputValidationIssues;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import org.springframework.web.multipart.MultipartFile;

final class GrantApplicationApiMapper {

    private static final String PROPOSED_AMOUNT = "/proposedAmount";

    private GrantApplicationApiMapper() {}

    static CreateApplicationCommand toCommand(CreateApplicationRequest request, Actor actor) {
        return new CreateApplicationCommand(
                new FundingCallId(request.getFundingCallId()),
                new OrganisationId(request.getOrganisationId()),
                toDomain(request.getProposal()),
                actor);
    }

    static EditApplicationCommand toCommand(
            UUID applicationId, be.achzak.civicgrant.api.generated.model.ProjectProposal proposal, Actor actor) {
        return new EditApplicationCommand(new ApplicationId(applicationId), toDomain(proposal), actor);
    }

    // A part without a file name or content type gets the domain's own refusal, not a 500.
    static AttachDocumentCommand toCommand(UUID applicationId, MultipartFile file, Actor actor) {
        return new AttachDocumentCommand(
                new ApplicationId(applicationId),
                Objects.requireNonNullElse(file.getOriginalFilename(), ""),
                Objects.requireNonNullElse(file.getContentType(), ""),
                contentOf(file),
                actor);
    }

    private static byte[] contentOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static RecordAssessmentCommand toCommand(UUID applicationId, AssessmentRequest request, Actor actor) {
        return new RecordAssessmentCommand(
                new ApplicationId(applicationId),
                request.getEligible(),
                toDomain(request.getRecommendation(), request.getProposedAmount()),
                request.getComment(),
                actor);
    }

    // The contract has no oneOf, so the mapper enforces "proposedAmount with APPROVE only".
    private static Recommendation toDomain(
            be.achzak.civicgrant.api.generated.model.Recommendation recommendation, BigDecimal proposedAmount) {
        return switch (recommendation) {
            case APPROVE -> {
                if (proposedAmount == null) {
                    throw new BadRequestProblem(InputValidationIssues.requiredInput(InEnum.BODY, PROPOSED_AMOUNT));
                }
                yield new Recommendation.Approve(new Money(proposedAmount));
            }
            case REJECT -> {
                if (proposedAmount != null) {
                    throw new BadRequestProblem(
                            InputValidationIssues.rejectedInput(InEnum.BODY, PROPOSED_AMOUNT, proposedAmount));
                }
                yield new Recommendation.Reject();
            }
        };
    }

    static be.achzak.civicgrant.api.generated.model.Assessment toResource(Assessment assessment) {
        return switch (assessment.recommendation()) {
            case Recommendation.Approve(Money proposedAmount) ->
                toResource(assessment, be.achzak.civicgrant.api.generated.model.Recommendation.APPROVE)
                        .proposedAmount(proposedAmount.amount());
            case Recommendation.Reject() ->
                toResource(assessment, be.achzak.civicgrant.api.generated.model.Recommendation.REJECT);
        };
    }

    private static be.achzak.civicgrant.api.generated.model.Assessment toResource(
            Assessment assessment, be.achzak.civicgrant.api.generated.model.Recommendation recommendation) {
        return new be.achzak.civicgrant.api.generated.model.Assessment(
                assessment.eligible(), recommendation, assessment.comment(), utc(assessment.assessedAt()));
    }

    static SearchApplicationsQuery toQuery(
            be.achzak.civicgrant.api.generated.model.ApplicationStatus status,
            UUID fundingCallId,
            int page,
            int pageSize,
            Actor actor) {
        Set<ApplicationStatus> statuses = status == null ? Set.of() : Set.of(ApplicationStatus.valueOf(status.name()));
        Set<FundingCallId> fundingCalls = fundingCallId == null ? Set.of() : Set.of(new FundingCallId(fundingCallId));
        return new SearchApplicationsQuery(statuses, fundingCalls, page, pageSize, actor);
    }

    static Application toResource(GrantApplication application) {
        Application resource = new Application(
                application.id().value(),
                application.reference().value(),
                application.fundingCallId().value(),
                application.organisationId().value(),
                toResource(application.status()),
                toResource(application.proposal()),
                application.documents().stream()
                        .map(GrantApplicationApiMapper::toResource)
                        .toList(),
                utc(application.createdAt()),
                utc(application.updatedAt()));
        application.submittedAt().map(GrantApplicationApiMapper::utc).ifPresent(resource::setSubmittedAt);
        return resource;
    }

    static be.achzak.civicgrant.api.generated.model.ApplicationPage toResource(
            ApplicationPage page, IntFunction<URI> linkToPage) {
        be.achzak.civicgrant.api.generated.model.ApplicationPage resource =
                new be.achzak.civicgrant.api.generated.model.ApplicationPage(
                        page.items().stream()
                                .map(GrantApplicationApiMapper::toResource)
                                .toList(),
                        page.page(),
                        page.pageSize(),
                        page.total(),
                        linkToPage.apply(page.page()));
        if ((long) page.page() * page.pageSize() < page.total()) {
            resource.setNext(linkToPage.apply(page.page() + 1));
        }
        if (page.page() > 1) {
            resource.setPrev(linkToPage.apply(page.page() - 1));
        }
        return resource;
    }

    static be.achzak.civicgrant.api.generated.model.ApplicationStatus toResource(ApplicationStatus status) {
        return be.achzak.civicgrant.api.generated.model.ApplicationStatus.valueOf(status.name());
    }

    static ProjectProposal toDomain(be.achzak.civicgrant.api.generated.model.ProjectProposal proposal) {
        return new ProjectProposal(
                proposal.getTitle(),
                proposal.getDescription(),
                new Money(proposal.getRequestedAmount()),
                new Money(proposal.getTotalProjectBudget()),
                Language.valueOf(proposal.getLanguage().name()));
    }

    private static be.achzak.civicgrant.api.generated.model.ProjectProposal toResource(ProjectProposal proposal) {
        return new be.achzak.civicgrant.api.generated.model.ProjectProposal(
                proposal.title(),
                proposal.description(),
                proposal.requestedAmount().amount(),
                proposal.totalProjectBudget().amount(),
                be.achzak.civicgrant.api.generated.model.Language.valueOf(
                        proposal.language().name()));
    }

    static Submission toSubmission(Instant submittedAt) {
        return new Submission(utc(submittedAt));
    }

    static be.achzak.civicgrant.api.generated.model.SupportingDocument toResource(SupportingDocument document) {
        return new be.achzak.civicgrant.api.generated.model.SupportingDocument(
                document.id().value(), document.fileName(), document.sizeInBytes(), utc(document.uploadedAt()));
    }

    private static be.achzak.civicgrant.api.generated.model.ApplicationSummary toResource(ApplicationSummary summary) {
        be.achzak.civicgrant.api.generated.model.ApplicationSummary resource =
                new be.achzak.civicgrant.api.generated.model.ApplicationSummary(
                        summary.id().value(),
                        summary.reference().value(),
                        summary.fundingCallId().value(),
                        summary.organisationId().value(),
                        toResource(summary.status()),
                        summary.projectTitle(),
                        summary.requestedAmount().amount(),
                        utc(summary.updatedAt()));
        if (summary.submittedAt() != null) {
            resource.setSubmittedAt(utc(summary.submittedAt()));
        }
        return resource;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
