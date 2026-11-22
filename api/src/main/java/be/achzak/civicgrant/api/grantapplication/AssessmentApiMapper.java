package be.achzak.civicgrant.api.grantapplication;

import static java.time.ZoneOffset.UTC;

import be.achzak.civicgrant.api.generated.model.AssessmentDto;
import be.achzak.civicgrant.api.generated.model.AssessmentRequestDto;
import be.achzak.civicgrant.api.generated.model.RecommendationDto;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import io.github.belgif.rest.problem.BadRequestProblem;
import io.github.belgif.rest.problem.api.InEnum;
import io.github.belgif.rest.problem.api.InputValidationIssues;
import java.math.BigDecimal;
import java.util.UUID;

final class AssessmentApiMapper {

    private static final String PROPOSED_AMOUNT = "/proposedAmount";

    private AssessmentApiMapper() {}

    static RecordAssessmentCommand toCommand(UUID applicationId, AssessmentRequestDto request, Actor actor) {
        return new RecordAssessmentCommand(
                new ApplicationId(applicationId),
                request.getEligible(),
                toDomain(request.getRecommendation(), request.getProposedAmount()),
                request.getComment(),
                actor);
    }

    static AssessmentDto toDto(Assessment assessment) {
        return switch (assessment.recommendation()) {
            case Recommendation.Approve(Money proposedAmount) ->
                toDto(assessment, RecommendationDto.APPROVE).proposedAmount(proposedAmount.amount());
            case Recommendation.Reject() -> toDto(assessment, RecommendationDto.REJECT);
        };
    }

    // The contract has no oneOf, so the mapper enforces "proposedAmount with APPROVE only".
    private static Recommendation toDomain(RecommendationDto recommendation, BigDecimal proposedAmount) {
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

    private static AssessmentDto toDto(Assessment assessment, RecommendationDto recommendation) {
        return new AssessmentDto(
                assessment.eligible(),
                recommendation,
                assessment.comment(),
                assessment.assessedAt().atOffset(UTC));
    }
}
