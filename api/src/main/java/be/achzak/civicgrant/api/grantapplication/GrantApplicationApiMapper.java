package be.achzak.civicgrant.api.grantapplication;

import static java.time.ZoneOffset.UTC;

import be.achzak.civicgrant.api.generated.model.ApplicationDto;
import be.achzak.civicgrant.api.generated.model.ApplicationPageDto;
import be.achzak.civicgrant.api.generated.model.ApplicationStatusDto;
import be.achzak.civicgrant.api.generated.model.ApplicationSummaryDto;
import be.achzak.civicgrant.api.generated.model.CreateApplicationRequestDto;
import be.achzak.civicgrant.api.generated.model.LanguageDto;
import be.achzak.civicgrant.api.generated.model.ProjectProposalDto;
import be.achzak.civicgrant.api.generated.model.SubmissionDto;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.EditApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.Language;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;

final class GrantApplicationApiMapper {

    private GrantApplicationApiMapper() {}

    static CreateApplicationCommand toCommand(CreateApplicationRequestDto request, Actor actor) {
        return new CreateApplicationCommand(
                new FundingCallId(request.getFundingCallId()),
                new OrganisationId(request.getOrganisationId()),
                toDomain(request.getProposal()),
                actor);
    }

    static EditApplicationCommand toCommand(UUID applicationId, ProjectProposalDto proposal, Actor actor) {
        return new EditApplicationCommand(new ApplicationId(applicationId), toDomain(proposal), actor);
    }

    static SearchApplicationsQuery toQuery(
            ApplicationStatusDto status, UUID fundingCallId, int page, int pageSize, Actor actor) {
        Set<ApplicationStatus> statuses = status == null ? Set.of() : Set.of(ApplicationStatus.valueOf(status.name()));
        Set<FundingCallId> fundingCalls = fundingCallId == null ? Set.of() : Set.of(new FundingCallId(fundingCallId));
        return new SearchApplicationsQuery(statuses, fundingCalls, page, pageSize, actor);
    }

    static ProjectProposal toDomain(ProjectProposalDto proposal) {
        return new ProjectProposal(
                proposal.getTitle(),
                proposal.getDescription(),
                new Money(proposal.getRequestedAmount()),
                new Money(proposal.getTotalProjectBudget()),
                Language.valueOf(proposal.getLanguage().name()));
    }

    static ApplicationDto toDto(GrantApplication application) {
        ApplicationDto dto = new ApplicationDto(
                application.id().value(),
                application.reference().value(),
                application.fundingCallId().value(),
                application.organisationId().value(),
                toDto(application.status()),
                toDto(application.proposal()),
                application.documents().stream()
                        .map(SupportingDocumentApiMapper::toDto)
                        .toList(),
                application.createdAt().atOffset(UTC),
                application.updatedAt().atOffset(UTC));
        application.submittedAt().ifPresent(submittedAt -> dto.setSubmittedAt(submittedAt.atOffset(UTC)));
        return dto;
    }

    static ApplicationPageDto toDto(ApplicationPage page, IntFunction<URI> linkToPage) {
        ApplicationPageDto dto = new ApplicationPageDto(
                page.items().stream().map(GrantApplicationApiMapper::toDto).toList(),
                page.page(),
                page.pageSize(),
                page.total(),
                linkToPage.apply(page.page()));
        if ((long) page.page() * page.pageSize() < page.total()) {
            dto.setNext(linkToPage.apply(page.page() + 1));
        }
        if (page.page() > 1) {
            dto.setPrev(linkToPage.apply(page.page() - 1));
        }
        return dto;
    }

    static ApplicationStatusDto toDto(ApplicationStatus status) {
        return ApplicationStatusDto.valueOf(status.name());
    }

    static SubmissionDto toSubmissionDto(Instant submittedAt) {
        return new SubmissionDto(submittedAt.atOffset(UTC));
    }

    private static ProjectProposalDto toDto(ProjectProposal proposal) {
        return new ProjectProposalDto(
                proposal.title(),
                proposal.description(),
                proposal.requestedAmount().amount(),
                proposal.totalProjectBudget().amount(),
                LanguageDto.valueOf(proposal.language().name()));
    }

    private static ApplicationSummaryDto toDto(ApplicationSummary summary) {
        ApplicationSummaryDto dto = new ApplicationSummaryDto(
                summary.id().value(),
                summary.reference().value(),
                summary.fundingCallId().value(),
                summary.organisationId().value(),
                toDto(summary.status()),
                summary.projectTitle(),
                summary.requestedAmount().amount(),
                summary.updatedAt().atOffset(UTC));
        if (summary.submittedAt() != null) {
            dto.setSubmittedAt(summary.submittedAt().atOffset(UTC));
        }
        return dto;
    }
}
