package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.SubmissionsApi;
import be.achzak.civicgrant.api.generated.model.Submission;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import io.github.belgif.rest.problem.ResourceNotFoundProblem;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class SubmissionController implements SubmissionsApi {

    private final SubmitApplicationUseCase submitApplication;
    private final GetApplicationUseCase getApplication;
    private final CurrentActor currentActor;

    SubmissionController(
            SubmitApplicationUseCase submitApplication,
            GetApplicationUseCase getApplication,
            CurrentActor currentActor) {
        this.submitApplication = submitApplication;
        this.getApplication = getApplication;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<Submission> submitApplication(UUID applicationId) {
        Actor actor = currentActor.get();
        ApplicationId id = new ApplicationId(applicationId);
        submitApplication.submit(new SubmitApplicationCommand(id, actor));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                        .build()
                        .toUri())
                .body(read(id, actor));
    }

    @Override
    public ResponseEntity<Submission> getSubmission(UUID applicationId) {
        return ResponseEntity.ok(read(new ApplicationId(applicationId), currentActor.get()));
    }

    // A draft has no submission yet: to the client, that's a resource that doesn't exist.
    private Submission read(ApplicationId id, Actor actor) {
        return getApplication
                .get(new GetApplicationQuery(id, actor))
                .submittedAt()
                .map(GrantApplicationApiMapper::toSubmission)
                .orElseThrow(ResourceNotFoundProblem::new);
    }
}
