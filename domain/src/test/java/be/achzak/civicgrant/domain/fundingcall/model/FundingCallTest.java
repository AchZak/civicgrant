package be.achzak.civicgrant.domain.fundingcall.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Funding call")
class FundingCallTest {

    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Nested
    @DisplayName("Committing a grant")
    class CommitGrant {

        @Test
        @DisplayName("takes the grant from the remaining budget")
        void takesTheGrantFromTheRemainingBudget() {
            // given
            FundingCall call = aFundingCall()
                    .withTotalBudget(euros("50000.00"))
                    .withCommitted(euros("30000.00"))
                    .build();

            // when
            call.commitGrant(euros("5000.00"), aDecisionMaker(), NOW);

            // then
            assertThat(call.remainingBudget()).isEqualTo(euros("15000.00"));
        }

        @Test
        @DisplayName("allows a grant of exactly the remaining budget")
        void allowsGrantOfExactlyTheRemainingBudget() {
            // given
            FundingCall call = aFundingCall()
                    .withTotalBudget(euros("50000.00"))
                    .withCommitted(euros("40000.00"))
                    .build();

            // when
            call.commitGrant(euros("10000.00"), aDecisionMaker(), NOW);

            // then
            assertThat(call.remainingBudget()).isEqualTo(Money.ZERO);
        }

        @Test
        @DisplayName("rejects a grant one cent above the remaining budget and leaves the budget untouched")
        void rejectsGrantOneCentAboveTheRemainingBudget() {
            // given
            FundingCall call = aFundingCall()
                    .withTotalBudget(euros("50000.00"))
                    .withCommitted(euros("40000.00"))
                    .build();

            // when
            Throwable thrown = catchThrowable(() -> call.commitGrant(euros("10000.01"), aDecisionMaker(), NOW));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "GRANT_EXCEEDS_REMAINING_BUDGET");
            assertThat(call.remainingBudget()).isEqualTo(euros("10000.00"));
        }

        @Test
        @DisplayName("rejects a grant of zero")
        void rejectsGrantOfZero() {
            // given
            FundingCall call = aFundingCall().build();

            // when
            Throwable thrown = catchThrowable(() -> call.commitGrant(euros("0.00"), aDecisionMaker(), NOW));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "GRANTED_AMOUNT_NOT_POSITIVE");
        }

        @Test
        @DisplayName("records GrantCommitted")
        void recordsGrantCommitted() {
            // given
            FundingCall call = aFundingCall().build();

            // when
            call.commitGrant(euros("5000.00"), aDecisionMaker(), NOW);

            // then
            assertThat(call.pullEvents())
                    .containsExactly(new GrantCommitted(
                            call.id(), euros("5000.00"), aDecisionMaker().id(), NOW));
        }
    }

    @Nested
    @DisplayName("Restoring")
    class Restore {

        @Test
        @DisplayName("records no events")
        void restoredCallHasNoEvents() {
            // given
            FundingCall call = aFundingCall().build();

            // when
            List<FundingCallEvent> events = call.pullEvents();

            // then
            assertThat(events).isEmpty();
        }
    }
}
