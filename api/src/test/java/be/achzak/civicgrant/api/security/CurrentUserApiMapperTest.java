package be.achzak.civicgrant.api.security;

import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.RoleDto;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Current user API mapping")
class CurrentUserApiMapperTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @DisplayName("maps every role of the domain to the contract")
    void mapsEveryRoleOfTheDomainToTheContract(Role role) {
        // when
        List<RoleDto> roles = CurrentUserApiMapper.toDto(actor(role)).getRoles();

        // then
        assertThat(roles).extracting(RoleDto::name).containsExactly(role.name());
    }
}
