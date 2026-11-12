package be.achzak.civicgrant.api.organisation;

import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.RegisterOrganisationRequest;
import be.achzak.civicgrant.domain.organisation.model.EnterpriseNumber;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.domain.shared.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Organisation API mapping")
class OrganisationApiMapperTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(be.achzak.civicgrant.api.generated.model.LegalForm.class)
    @DisplayName("maps every legal form of the contract to the domain")
    void mapsEveryLegalFormOfTheContractToTheDomain(be.achzak.civicgrant.api.generated.model.LegalForm legalForm) {
        // given
        RegisterOrganisationRequest request = new RegisterOrganisationRequest("0202239951", "Buurtwerk vzw", legalForm);

        // when
        RegisterOrganisationCommand command = OrganisationApiMapper.toCommand(request, actor(Role.APPLICANT));

        // then
        assertThat(command.legalForm().name()).isEqualTo(legalForm.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LegalForm.class)
    @DisplayName("maps every legal form of the domain to the contract")
    void mapsEveryLegalFormOfTheDomainToTheContract(LegalForm legalForm) {
        // given
        Organisation organisation = Organisation.restore(
                OrganisationId.generate(),
                new EnterpriseNumber("0202239951"),
                "Buurtwerk vzw",
                legalForm,
                Instant.parse("2026-01-10T09:00:00Z"));

        // when
        String mapped =
                OrganisationApiMapper.toResource(organisation).getLegalForm().name();

        // then
        assertThat(mapped).isEqualTo(legalForm.name());
    }
}
