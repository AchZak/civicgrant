package be.achzak.civicgrant.api.organisation;

import be.achzak.civicgrant.api.generated.model.LegalFormDto;
import be.achzak.civicgrant.api.generated.model.OrganisationCollectionDto;
import be.achzak.civicgrant.api.generated.model.OrganisationDto;
import be.achzak.civicgrant.api.generated.model.RegisterOrganisationRequestDto;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.Organisation;
import java.util.List;

final class OrganisationApiMapper {

    private OrganisationApiMapper() {}

    static RegisterOrganisationCommand toCommand(RegisterOrganisationRequestDto request, Actor actor) {
        return new RegisterOrganisationCommand(
                EnterpriseNumber.parse(request.getEnterpriseNumber()),
                request.getName(),
                LegalForm.valueOf(request.getLegalForm().name()),
                actor);
    }

    static OrganisationCollectionDto toDto(List<Organisation> organisations) {
        return new OrganisationCollectionDto(
                organisations.stream().map(OrganisationApiMapper::toDto).toList());
    }

    static OrganisationDto toDto(Organisation organisation) {
        return new OrganisationDto(
                organisation.id().value(),
                organisation.enterpriseNumber().value(),
                organisation.name(),
                LegalFormDto.valueOf(organisation.legalForm().name()));
    }
}
