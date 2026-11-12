package be.achzak.civicgrant.api.organisation;

import be.achzak.civicgrant.api.generated.model.OrganisationCollection;
import be.achzak.civicgrant.api.generated.model.RegisterOrganisationRequest;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.shared.Actor;
import java.util.List;

final class OrganisationApiMapper {

    private OrganisationApiMapper() {}

    static RegisterOrganisationCommand toCommand(RegisterOrganisationRequest request, Actor actor) {
        return new RegisterOrganisationCommand(
                EnterpriseNumber.parse(request.getEnterpriseNumber()),
                request.getName(),
                LegalForm.valueOf(request.getLegalForm().name()),
                actor);
    }

    static OrganisationCollection toCollection(List<Organisation> organisations) {
        return new OrganisationCollection(
                organisations.stream().map(OrganisationApiMapper::toResource).toList());
    }

    static be.achzak.civicgrant.api.generated.model.Organisation toResource(Organisation organisation) {
        return new be.achzak.civicgrant.api.generated.model.Organisation(
                organisation.id().value(),
                organisation.enterpriseNumber().value(),
                organisation.name(),
                be.achzak.civicgrant.api.generated.model.LegalForm.valueOf(
                        organisation.legalForm().name()));
    }
}
