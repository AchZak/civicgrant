package be.achzak.civicgrant.api.grantapplication;

import static java.time.ZoneOffset.UTC;

import be.achzak.civicgrant.api.generated.model.DecisionDto;
import be.achzak.civicgrant.api.generated.model.DecisionOutcomeDto;
import be.achzak.civicgrant.api.generated.model.DecisionRequestDto;
import be.achzak.civicgrant.api.generated.model.RejectionGroundDto;
import be.achzak.civicgrant.core.common.domain.Money;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.Decision;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import io.github.belgif.rest.problem.BadRequestProblem;
import io.github.belgif.rest.problem.api.InEnum;
import io.github.belgif.rest.problem.api.InputValidationIssues;
import java.util.UUID;

// The contract has no oneOf, so the mapper enforces "grantedAmount for an approval, rejectionGround for a
// rejection, never the other one".
final class DecisionApiMapper {

    private static final String GRANTED_AMOUNT = "/grantedAmount";
    private static final String REJECTION_GROUND = "/rejectionGround";

    private DecisionApiMapper() {}

    static ApproveApplicationCommand toApproveCommand(UUID applicationId, DecisionRequestDto request, Actor actor) {
        if (request.getGrantedAmount() == null) {
            throw new BadRequestProblem(InputValidationIssues.requiredInput(InEnum.BODY, GRANTED_AMOUNT));
        }
        if (request.getRejectionGround() != null) {
            throw new BadRequestProblem(
                    InputValidationIssues.rejectedInput(InEnum.BODY, REJECTION_GROUND, request.getRejectionGround()));
        }
        return new ApproveApplicationCommand(
                new ApplicationId(applicationId),
                new Money(request.getGrantedAmount()),
                request.getMotivation(),
                actor);
    }

    static RejectApplicationCommand toRejectCommand(UUID applicationId, DecisionRequestDto request, Actor actor) {
        if (request.getRejectionGround() == null) {
            throw new BadRequestProblem(InputValidationIssues.requiredInput(InEnum.BODY, REJECTION_GROUND));
        }
        if (request.getGrantedAmount() != null) {
            throw new BadRequestProblem(
                    InputValidationIssues.rejectedInput(InEnum.BODY, GRANTED_AMOUNT, request.getGrantedAmount()));
        }
        return new RejectApplicationCommand(
                new ApplicationId(applicationId),
                toDomain(request.getRejectionGround()),
                request.getMotivation(),
                actor);
    }

    static RejectionGround toDomain(RejectionGroundDto ground) {
        return RejectionGround.valueOf(ground.name());
    }

    static DecisionDto toDto(Decision decision) {
        return switch (decision) {
            case Decision.Approval approval ->
                toDto(decision, DecisionOutcomeDto.APPROVED)
                        .grantedAmount(approval.grantedAmount().amount());
            case Decision.Rejection rejection ->
                toDto(decision, DecisionOutcomeDto.REJECTED).rejectionGround(toDto(rejection.ground()));
        };
    }

    static RejectionGroundDto toDto(RejectionGround ground) {
        return RejectionGroundDto.valueOf(ground.name());
    }

    private static DecisionDto toDto(Decision decision, DecisionOutcomeDto outcome) {
        return new DecisionDto(
                outcome, decision.motivation(), decision.decidedAt().atOffset(UTC));
    }
}
