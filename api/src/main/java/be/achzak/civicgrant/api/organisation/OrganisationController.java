package be.achzak.civicgrant.api.organisation;

import be.achzak.civicgrant.api.generated.api.OrganisationsApi;
import be.achzak.civicgrant.api.generated.model.OrganisationCollectionDto;
import be.achzak.civicgrant.api.generated.model.OrganisationDto;
import be.achzak.civicgrant.api.generated.model.RegisterOrganisationRequestDto;
import be.achzak.civicgrant.api.security.CurrentActor;
import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationUseCase;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsUseCase;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class OrganisationController implements OrganisationsApi {

    private final RegisterOrganisationUseCase registerOrganisation;
    private final GetOrganisationUseCase getOrganisation;
    private final SearchOrganisationsUseCase searchOrganisations;
    private final CurrentActor currentActor;

    OrganisationController(
            RegisterOrganisationUseCase registerOrganisation,
            GetOrganisationUseCase getOrganisation,
            SearchOrganisationsUseCase searchOrganisations,
            CurrentActor currentActor) {
        this.registerOrganisation = registerOrganisation;
        this.getOrganisation = getOrganisation;
        this.searchOrganisations = searchOrganisations;
        this.currentActor = currentActor;
    }

    @Override
    public ResponseEntity<OrganisationDto> registerOrganisation(RegisterOrganisationRequestDto request) {
        Actor actor = currentActor.get();
        OrganisationId id = registerOrganisation.register(OrganisationApiMapper.toCommand(request, actor));
        OrganisationDto registered =
                OrganisationApiMapper.toDto(getOrganisation.get(new GetOrganisationQuery(id, actor)));
        return ResponseEntity.created(locationOf(id)).body(registered);
    }

    @Override
    public ResponseEntity<OrganisationDto> getOrganisation(UUID organisationId) {
        GetOrganisationQuery query = new GetOrganisationQuery(new OrganisationId(organisationId), currentActor.get());
        return ResponseEntity.ok(OrganisationApiMapper.toDto(getOrganisation.get(query)));
    }

    @Override
    public ResponseEntity<OrganisationCollectionDto> findOrganisations(String enterpriseNumber) {
        SearchOrganisationsQuery query =
                new SearchOrganisationsQuery(EnterpriseNumber.parse(enterpriseNumber), currentActor.get());
        return ResponseEntity.ok(OrganisationApiMapper.toDto(searchOrganisations.search(query)));
    }

    private static URI locationOf(OrganisationId id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{organisationId}")
                .buildAndExpand(id.value())
                .toUri();
    }
}
