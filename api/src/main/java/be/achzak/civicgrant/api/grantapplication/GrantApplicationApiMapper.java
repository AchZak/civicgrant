package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.model.Application;
import be.achzak.civicgrant.api.generated.model.CreateApplicationRequest;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Money;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;

final class GrantApplicationApiMapper {

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

    private static be.achzak.civicgrant.api.generated.model.SupportingDocument toResource(SupportingDocument document) {
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
