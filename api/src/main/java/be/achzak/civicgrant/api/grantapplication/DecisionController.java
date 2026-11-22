package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.DecisionsApi;
import be.achzak.civicgrant.api.generated.model.DecisionDto;
import be.achzak.civicgrant.api.generated.model.DecisionRequestDto;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import io.github.belgif.rest.problem.ResourceNotFoundProblem;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class DecisionController implements DecisionsApi {

    private final ApproveApplicationUseCase approveApplication;
    private final RejectApplicationUseCase rejectApplication;
    private final GetApplicationUseCase getApplication;
    private final CurrentActor currentActor;

    DecisionController(
            ApproveApplicationUseCase approveApplication,
            RejectApplicationUseCase rejectApplication,
            GetApplicationUseCase getApplication,
            CurrentActor currentActor) {
        this.approveApplication = approveApplication;
        this.rejectApplication = rejectApplication;
        this.getApplication = getApplication;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<DecisionDto> decideApplication(UUID applicationId, DecisionRequestDto decisionRequest) {
        Actor actor = currentActor.get();
        switch (decisionRequest.getOutcome()) {
            case APPROVED ->
                approveApplication.approve(DecisionApiMapper.toApproveCommand(applicationId, decisionRequest, actor));
            case REJECTED ->
                rejectApplication.reject(DecisionApiMapper.toRejectCommand(applicationId, decisionRequest, actor));
        }
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                        .build()
                        .toUri())
                .body(read(new ApplicationId(applicationId), actor));
    }

    @Override
    public ResponseEntity<DecisionDto> getDecision(UUID applicationId) {
        return ResponseEntity.ok(read(new ApplicationId(applicationId), currentActor.get()));
    }

    // An application without a decision yet: to the client, that's a resource that doesn't exist.
    private DecisionDto read(ApplicationId id, Actor actor) {
        return getApplication
                .get(new GetApplicationQuery(id, actor))
                .decision()
                .map(DecisionApiMapper::toDto)
                .orElseThrow(ResourceNotFoundProblem::new);
    }
}
