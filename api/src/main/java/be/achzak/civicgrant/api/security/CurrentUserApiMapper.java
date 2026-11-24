package be.achzak.civicgrant.api.security;

import be.achzak.civicgrant.api.generated.model.CurrentUserDto;
import be.achzak.civicgrant.api.generated.model.RoleDto;
import be.achzak.civicgrant.core.common.domain.identity.Actor;

final class CurrentUserApiMapper {

    private CurrentUserApiMapper() {}

    static CurrentUserDto toDto(Actor actor) {
        return new CurrentUserDto(
                actor.id().value(),
                actor.roles().stream()
                        .sorted()
                        .map(role -> RoleDto.valueOf(role.name()))
                        .toList());
    }
}
