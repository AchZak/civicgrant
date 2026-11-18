package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.AssessmentsApi;
import be.achzak.civicgrant.api.generated.model.Assessment;
import be.achzak.civicgrant.api.generated.model.AssessmentRequest;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class AssessmentController implements AssessmentsApi {

    private final RecordAssessmentUseCase recordAssessment;
    private final GetAssessmentUseCase getAssessment;
    private final CurrentActor currentActor;

    AssessmentController(
            RecordAssessmentUseCase recordAssessment, GetAssessmentUseCase getAssessment, CurrentActor currentActor) {
        this.recordAssessment = recordAssessment;
        this.getAssessment = getAssessment;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<Assessment> recordAssessment(UUID applicationId, AssessmentRequest assessmentRequest) {
        Actor actor = currentActor.get();
        recordAssessment.record(GrantApplicationApiMapper.toCommand(applicationId, assessmentRequest, actor));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                        .build()
                        .toUri())
                .body(read(new ApplicationId(applicationId), actor));
    }

    @Override
    public ResponseEntity<Assessment> getAssessment(UUID applicationId) {
        return ResponseEntity.ok(read(new ApplicationId(applicationId), currentActor.get()));
    }

    private Assessment read(ApplicationId id, Actor actor) {
        return GrantApplicationApiMapper.toResource(getAssessment.get(new GetAssessmentQuery(id, actor)));
    }
}
