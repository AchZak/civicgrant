package be.achzak.civicgrant.api.grantapplication;

import be.achzak.civicgrant.api.generated.api.ApplicationsApi;
import be.achzak.civicgrant.api.generated.model.Application;
import be.achzak.civicgrant.api.generated.model.ApplicationPage;
import be.achzak.civicgrant.api.generated.model.ApplicationStatus;
import be.achzak.civicgrant.api.generated.model.CreateApplicationRequest;
import be.achzak.civicgrant.api.generated.model.ProjectProposal;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsUseCase;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import io.github.belgif.rest.problem.BadRequestProblem;
import io.github.belgif.rest.problem.api.InEnum;
import io.github.belgif.rest.problem.api.InputValidationIssues;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class GrantApplicationController implements ApplicationsApi {

    private final CreateApplicationUseCase createApplication;
    private final EditApplicationUseCase editApplication;
    private final GetApplicationUseCase getApplication;
    private final SearchApplicationsUseCase searchApplications;
    private final CurrentActor currentActor;

    GrantApplicationController(
            CreateApplicationUseCase createApplication,
            EditApplicationUseCase editApplication,
            GetApplicationUseCase getApplication,
            SearchApplicationsUseCase searchApplications,
            CurrentActor currentActor) {
        this.createApplication = createApplication;
        this.editApplication = editApplication;
        this.getApplication = getApplication;
        this.searchApplications = searchApplications;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<Application> createApplication(CreateApplicationRequest request) {
        Actor actor = currentActor.get();
        ApplicationId id = create(request, actor);
        return ResponseEntity.created(locationOf(id)).body(read(id, actor));
    }

    @Override
    public ResponseEntity<Application> editApplication(UUID applicationId, ProjectProposal proposal) {
        Actor actor = currentActor.get();
        editApplication.edit(GrantApplicationApiMapper.toCommand(applicationId, proposal, actor));
        return ResponseEntity.ok(read(new ApplicationId(applicationId), actor));
    }

    @Override
    public ResponseEntity<Application> getApplication(UUID applicationId) {
        return ResponseEntity.ok(read(new ApplicationId(applicationId), currentActor.get()));
    }

    @Override
    public ResponseEntity<ApplicationPage> listApplications(
            ApplicationStatus status, UUID fundingCallId, Integer page, Integer pageSize) {
        SearchApplicationsQuery query =
                GrantApplicationApiMapper.toQuery(status, fundingCallId, page, pageSize, currentActor.get());
        return ResponseEntity.ok(GrantApplicationApiMapper.toResource(
                searchApplications.search(query), linkToPage(status, fundingCallId, pageSize)));
    }

    private ApplicationId create(CreateApplicationRequest request, Actor actor) {
        try {
            return createApplication.create(GrantApplicationApiMapper.toCommand(request, actor));
        } catch (NotFoundException e) {
            throw referenceNotFound(e, request);
        }
    }

    // A missing call or organisation is a broken reference in the body, not a missing resource at this URL.
    private static RuntimeException referenceNotFound(NotFoundException exception, CreateApplicationRequest request) {
        return switch (exception.code()) {
            case "FUNDING_CALL_NOT_FOUND" ->
                new BadRequestProblem(InputValidationIssues.referencedResourceNotFound(
                        InEnum.BODY, "/fundingCallId", request.getFundingCallId()));
            case "ORGANISATION_NOT_FOUND" ->
                new BadRequestProblem(InputValidationIssues.referencedResourceNotFound(
                        InEnum.BODY, "/organisationId", request.getOrganisationId()));
            default -> exception;
        };
    }

    private Application read(ApplicationId id, Actor actor) {
        return GrantApplicationApiMapper.toResource(getApplication.get(new GetApplicationQuery(id, actor)));
    }

    private static URI locationOf(ApplicationId id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{applicationId}")
                .buildAndExpand(id.value())
                .toUri();
    }

    private static IntFunction<URI> linkToPage(ApplicationStatus status, UUID fundingCallId, int pageSize) {
        return page -> ServletUriComponentsBuilder.fromCurrentRequestUri()
                .queryParamIfPresent("status", Optional.ofNullable(status))
                .queryParamIfPresent("fundingCallId", Optional.ofNullable(fundingCallId))
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .build()
                .toUri();
    }
}
