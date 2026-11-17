package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSummary;
import be.achzak.civicgrant.domain.grantapplication.model.Assessment;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationSnapshot;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.shared.Money;
import be.achzak.civicgrant.domain.shared.UserId;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class GrantApplicationJpaMapper {

    private GrantApplicationJpaMapper() {}

    static GrantApplication toDomain(GrantApplicationJpaEntity entity) {
        // The decision lives in its own table, not mapped yet.
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
                entity.getDocuments().stream()
                        .map(GrantApplicationJpaMapper::toDomain)
                        .toList(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                entity.getAssessment() == null ? null : toDomain(entity.getAssessment()),
                null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }

    static ApplicationSummary toSummary(GrantApplicationSummaryRow row) {
        return new ApplicationSummary(
                new ApplicationId(row.id()),
                new ApplicationReference(row.reference()),
                new FundingCallId(row.fundingCallId()),
                new OrganisationId(row.organisationId()),
                row.status(),
                row.projectTitle(),
                new Money(row.requestedAmount()),
                row.submittedAt(),
                row.updatedAt());
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
        updateDocuments(entity, application);
        if (entity.getAssessment() == null) {
            application
                    .assessment()
                    .map(assessment -> toEntity(entity, assessment))
                    .ifPresent(entity::setAssessment);
        }
    }

    private static AssessmentJpaEntity toEntity(GrantApplicationJpaEntity application, Assessment assessment) {
        return switch (assessment.recommendation()) {
            case Recommendation.Approve(Money proposedAmount) ->
                toEntity(application, assessment, AssessmentRecommendation.APPROVE, proposedAmount.amount());
            case Recommendation.Reject() -> toEntity(application, assessment, AssessmentRecommendation.REJECT, null);
        };
    }

    private static AssessmentJpaEntity toEntity(
            GrantApplicationJpaEntity application,
            Assessment assessment,
            AssessmentRecommendation recommendation,
            BigDecimal proposedAmount) {
        return new AssessmentJpaEntity(
                application,
                assessment.assessor().value(),
                assessment.eligible(),
                recommendation,
                proposedAmount,
                assessment.comment(),
                assessment.assessedAt());
    }

    private static Assessment toDomain(AssessmentJpaEntity entity) {
        Recommendation recommendation = switch (entity.getRecommendation()) {
            case APPROVE -> new Recommendation.Approve(new Money(entity.getProposedAmount()));
            case REJECT -> new Recommendation.Reject();
        };
        return new Assessment(
                new UserId(entity.getAssessorOid()),
                entity.isEligible(),
                recommendation,
                entity.getComment(),
                entity.getAssessedAt());
    }

    private static void updateDocuments(GrantApplicationJpaEntity entity, GrantApplication application) {
        Set<UUID> kept = application.documents().stream()
                .map(document -> document.id().value())
                .collect(Collectors.toSet());
        entity.getDocuments().removeIf(stored -> !kept.contains(stored.getId()));
        Set<UUID> stored = entity.getDocuments().stream()
                .map(SupportingDocumentJpaEntity::getId)
                .collect(Collectors.toSet());
        application.documents().stream()
                .filter(document -> !stored.contains(document.id().value()))
                .map(document -> toEntity(application.id(), document))
                .forEach(entity.getDocuments()::add);
    }

    private static SupportingDocumentJpaEntity toEntity(ApplicationId applicationId, SupportingDocument document) {
        return new SupportingDocumentJpaEntity(
                document.id().value(),
                DocumentBlobNames.of(applicationId, document.id()),
                document.fileName(),
                SupportingDocument.CONTENT_TYPE,
                document.sizeInBytes(),
                document.uploadedBy().value(),
                document.uploadedAt());
    }

    private static SupportingDocument toDomain(SupportingDocumentJpaEntity entity) {
        return new SupportingDocument(
                new DocumentId(entity.getId()),
                entity.getOriginalFilename(),
                entity.getSizeBytes(),
                new UserId(entity.getUploadedByOid()),
                entity.getUploadedAt());
    }
}
