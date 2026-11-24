package be.achzak.civicgrant.api.security;

import be.achzak.civicgrant.api.generated.api.CurrentUserApi;
import be.achzak.civicgrant.api.generated.model.CurrentUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// No use case: the answer is the access token itself, so nothing is loaded and no rule applies.
@RestController
@RequiredArgsConstructor
class CurrentUserController implements CurrentUserApi {

    private final CurrentActor currentActor;

    @Override
    public ResponseEntity<CurrentUserDto> getCurrentUser() {
        return ResponseEntity.ok(CurrentUserApiMapper.toDto(currentActor.get()));
    }
}
