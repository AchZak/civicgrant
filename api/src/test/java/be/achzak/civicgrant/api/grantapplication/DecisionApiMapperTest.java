package be.achzak.civicgrant.api.grantapplication;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.RejectionGroundDto;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Decision API mapping")
class DecisionApiMapperTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(RejectionGroundDto.class)
    @DisplayName("maps every rejection ground of the contract to the domain")
    void mapsEveryRejectionGroundOfTheContractToTheDomain(RejectionGroundDto ground) {
        // when
        RejectionGround mapped = DecisionApiMapper.toDomain(ground);

        // then
        assertThat(mapped.name()).isEqualTo(ground.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RejectionGround.class)
    @DisplayName("maps every rejection ground of the domain to the contract")
    void mapsEveryRejectionGroundOfTheDomainToTheContract(RejectionGround ground) {
        // when
        String mapped = DecisionApiMapper.toDto(ground).name();

        // then
        assertThat(mapped).isEqualTo(ground.name());
    }
}
