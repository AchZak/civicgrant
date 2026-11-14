package be.achzak.civicgrant.domain.grantapplication.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Year;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application reference")
class ApplicationReferenceTest {

    @Test
    @DisplayName("is built from the year and a number padded to six digits")
    void isBuiltFromTheYearAndANumberPaddedToSixDigits() {
        // when
        ApplicationReference reference = ApplicationReference.of(Year.of(2026), 42);

        // then
        assertThat(reference.value()).isEqualTo("CG-2026-000042");
    }

    @Test
    @DisplayName("refuses a number that needs more than six digits")
    void refusesANumberThatNeedsMoreThanSixDigits() {
        // when
        Throwable thrown = catchThrowable(() -> ApplicationReference.of(Year.of(2026), 1_000_000));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses a value that doesn't look like CG-2026-000042")
    void refusesAValueThatDoesNotLookLikeAReference() {
        // when
        Throwable thrown = catchThrowable(() -> new ApplicationReference("CG-26-42"));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
