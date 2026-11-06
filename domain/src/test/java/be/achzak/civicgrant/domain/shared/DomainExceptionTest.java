package be.achzak.civicgrant.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain exception")
class DomainExceptionTest {

    @Test
    @DisplayName("carries its code")
    void carriesItsCode() {
        // when
        DomainException exception = new ConflictException("APPLICATION_NOT_EDITABLE", "Only a draft can be edited");

        // then
        assertThat(exception.code()).isEqualTo("APPLICATION_NOT_EDITABLE");
    }

    @Test
    @DisplayName("rejects a code that isn't UPPER_SNAKE_CASE")
    void rejectsCodeThatIsNotUpperSnakeCase() {
        // when
        Throwable thrown = catchThrowable(() -> new NotFoundException("applicationNotFound", "No such application"));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
