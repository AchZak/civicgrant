package be.achzak.civicgrant.core.common.domain.error;

import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.CONCURRENT_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain exception")
class DomainExceptionTest {

    @Test
    @DisplayName("carries its code")
    void carriesItsCode() {
        // when
        DomainException exception = new ConflictException(CONCURRENT_UPDATE, "Changed by someone else");

        // then
        assertThat(exception.code()).isEqualTo(CONCURRENT_UPDATE);
    }
}
