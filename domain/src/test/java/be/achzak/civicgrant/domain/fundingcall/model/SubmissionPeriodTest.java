package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.CLOSES_AT;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.OPENS_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Submission period")
class SubmissionPeriodTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("closes after it opens")
        void closesAfterItOpens() {
            // when
            Throwable thrown = catchThrowable(() -> new SubmissionPeriod(OPENS_AT, OPENS_AT));

            // then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Status at a given moment")
    class Status {

        private final SubmissionPeriod period = new SubmissionPeriod(OPENS_AT, CLOSES_AT);

        @Test
        @DisplayName("is upcoming and not yet open just before it opens")
        void isUpcomingBeforeItOpens() {
            // given
            Instant justBeforeOpening = OPENS_AT.minusSeconds(1);

            // when
            boolean upcoming = period.isUpcomingAt(justBeforeOpening);
            boolean open = period.isOpenAt(justBeforeOpening);

            // then
            assertThat(upcoming).isTrue();
            assertThat(open).isFalse();
        }

        @Test
        @DisplayName("is open from the opening moment")
        void isOpenFromTheOpeningMoment() {
            // when
            boolean open = period.isOpenAt(OPENS_AT);

            // then
            assertThat(open).isTrue();
        }

        @Test
        @DisplayName("is still open just before closing")
        void isOpenUntilJustBeforeClosing() {
            // given
            Instant justBeforeClosing = CLOSES_AT.minusSeconds(1);

            // when
            boolean open = period.isOpenAt(justBeforeClosing);

            // then
            assertThat(open).isTrue();
        }

        @Test
        @DisplayName("is closed, and no longer open, from the closing moment")
        void isClosedFromTheClosingMoment() {
            // when
            boolean closed = period.isClosedAt(CLOSES_AT);
            boolean open = period.isOpenAt(CLOSES_AT);

            // then
            assertThat(closed).isTrue();
            assertThat(open).isFalse();
        }
    }
}
