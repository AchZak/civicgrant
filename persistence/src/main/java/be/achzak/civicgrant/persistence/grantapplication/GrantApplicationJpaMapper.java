package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationSnapshot;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.util.List;

final class GrantApplicationJpaMapper {

    private GrantApplicationJpaMapper() {}

    static GrantApplication toDomain(GrantApplicationJpaEntity entity) {
        // Documents, the assessment and the decision live in their own tables, not mapped yet.
        return GrantApplication.restore(new GrantApplicationSnapshot(
                new ApplicationId(entity.getId()),
                new ApplicationReference(entity.getReference()),
                new FundingCallId(entity.getFundingCallId()),
                new OrganisationId(entity.getOrganisationId()),
                new UserId(entity.getApplicantOid()),
                new ProjectProposal(
                        entity.getProjectTitle(),
                        entity.getProjectDescription(),
                        new Money(entity.getRequestedAmount()),
                        new Money(entity.getTotalProjectBudget()),
                        entity.getLanguage()),
                List.of(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                null,
                null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }

    static GrantApplicationJpaEntity toEntity(GrantApplication application) {
        GrantApplicationJpaEntity entity = new GrantApplicationJpaEntity(
                application.id().value(),
                application.reference().value(),
                application.fundingCallId().value(),
                application.organisationId().value(),
                application.applicant().value(),
                application.createdAt());
        update(entity, application);
        return entity;
    }

    static void update(GrantApplicationJpaEntity entity, GrantApplication application) {
        ProjectProposal proposal = application.proposal();
        entity.setProjectTitle(proposal.title());
        entity.setProjectDescription(proposal.description());
        entity.setRequestedAmount(proposal.requestedAmount().amount());
        entity.setTotalProjectBudget(proposal.totalProjectBudget().amount());
        entity.setLanguage(proposal.language());
        entity.setStatus(application.status());
        entity.setSubmittedAt(application.submittedAt().orElse(null));
        entity.setUpdatedAt(application.updatedAt());
    }
}
