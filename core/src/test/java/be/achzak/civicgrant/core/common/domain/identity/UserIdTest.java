package be.achzak.civicgrant.core.common.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("User id")
class UserIdTest {

    static Stream<String> invalidIds() {
        return Stream.of(" ", "x".repeat(65));
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    @DisplayName("refuses a blank id or one longer than 64 characters")
    void refusesABlankIdOrOneLongerThan64Characters(String value) {
        // when
        Throwable thrown = catchThrowable(() -> new UserId(value));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
