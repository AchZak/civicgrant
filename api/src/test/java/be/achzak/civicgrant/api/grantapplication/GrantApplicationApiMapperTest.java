package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aDraft;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aProposal;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.shared.Role;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Grant application API mapping")
class GrantApplicationApiMapperTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(be.achzak.civicgrant.api.generated.model.Language.class)
    @DisplayName("maps every language of the contract to the domain")
    void mapsEveryLanguageOfTheContractToTheDomain(be.achzak.civicgrant.api.generated.model.Language language) {
        // given
        be.achzak.civicgrant.api.generated.model.ProjectProposal proposal =
                new be.achzak.civicgrant.api.generated.model.ProjectProposal(
                        "Samen tuinieren", "Een moestuin.", new BigDecimal("8000"), new BigDecimal("12000"), language);

        // when
        ProjectProposal mapped = GrantApplicationApiMapper.toDomain(proposal);

        // then
        assertThat(mapped.language().name()).isEqualTo(language.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Language.class)
    @DisplayName("maps every language of the domain to the contract")
    void mapsEveryLanguageOfTheDomainToTheContract(Language language) {
        // when
        String mapped = GrantApplicationApiMapper.toResource(aDraft(aProposal(language)))
                .getProposal()
                .getLanguage()
                .name();

        // then
        assertThat(mapped).isEqualTo(language.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(be.achzak.civicgrant.api.generated.model.ApplicationStatus.class)
    @DisplayName("maps every status of the contract to the domain")
    void mapsEveryStatusOfTheContractToTheDomain(be.achzak.civicgrant.api.generated.model.ApplicationStatus status) {
        // when
        SearchApplicationsQuery query = GrantApplicationApiMapper.toQuery(status, null, 1, 20, actor(Role.ASSESSOR));

        // then
        assertThat(query.statuses()).extracting(Enum::name).containsExactly(status.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ApplicationStatus.class)
    @DisplayName("maps every status of the domain to the contract")
    void mapsEveryStatusOfTheDomainToTheContract(ApplicationStatus status) {
        // when
        String mapped = GrantApplicationApiMapper.toResource(status).name();

        // then
        assertThat(mapped).isEqualTo(status.name());
    }
}
