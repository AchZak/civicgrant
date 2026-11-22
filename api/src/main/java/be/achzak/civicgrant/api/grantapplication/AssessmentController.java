package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.AssessmentsApi;
import be.achzak.civicgrant.api.generated.model.AssessmentDto;
import be.achzak.civicgrant.api.generated.model.AssessmentRequestDto;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
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
    public ResponseEntity<AssessmentDto> recordAssessment(UUID applicationId, AssessmentRequestDto assessmentRequest) {
        Actor actor = currentActor.get();
        recordAssessment.record(AssessmentApiMapper.toCommand(applicationId, assessmentRequest, actor));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                        .build()
                        .toUri())
                .body(read(new ApplicationId(applicationId), actor));
    }

    @Override
    public ResponseEntity<AssessmentDto> getAssessment(UUID applicationId) {
        return ResponseEntity.ok(read(new ApplicationId(applicationId), currentActor.get()));
    }

    private AssessmentDto read(ApplicationId id, Actor actor) {
        return AssessmentApiMapper.toDto(getAssessment.get(new GetAssessmentQuery(id, actor)));
    }
}
