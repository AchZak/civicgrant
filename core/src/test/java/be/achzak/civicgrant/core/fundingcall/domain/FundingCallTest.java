package be.achzak.civicgrant.core.fundingcall.domain;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.aDecisionMaker;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallEvent;
import be.achzak.civicgrant.core.fundingcall.domain.event.FundingCallOpened;
import be.achzak.civicgrant.core.fundingcall.domain.event.GrantCommitted;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.LocalizedText;
import be.achzak.civicgrant.core.sharedkernel.domain.Money;
import java.time.Duration;
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
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANT_EXCEEDS_REMAINING_BUDGET");
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
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("GRANTED_AMOUNT_NOT_POSITIVE");
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
    @DisplayName("Opening a call")
    class Create {

        // The aggregate only records who opened the call; the use case checks the role.
        private static final Actor OPENED_BY = actor("programme-manager-1");
        private static final FundingCallReference REFERENCE = new FundingCallReference("GRN-2027");
        private static final LocalizedText TITLE = new LocalizedText("Groene daken 2027", "Toitures vertes 2027");
        private static final SubmissionPeriod PERIOD =
                new SubmissionPeriod(NOW.plus(Duration.ofDays(14)), NOW.plus(Duration.ofDays(120)));

        private static FundingCall open(LocalizedText title, SubmissionPeriod period) {
            return FundingCall.create(REFERENCE, title, period, euros("80000.00"), euros("20000.00"), OPENED_BY, NOW);
        }

        @Test
        @DisplayName("opens an unspent call, stored for the first time, with the trimmed titles")
        void opensAnUnspentCallWithTheTrimmedTitles() {
            // when
            FundingCall call = open(new LocalizedText(" Groene daken 2027 ", "Toitures vertes 2027  "), PERIOD);

            // then
            assertThat(call)
                    .extracting(
                            FundingCall::reference,
                            FundingCall::title,
                            FundingCall::submissionPeriod,
                            FundingCall::budget,
                            FundingCall::version,
                            FundingCall::createdAt)
                    .containsExactly(
                            REFERENCE,
                            TITLE,
                            PERIOD,
                            new BudgetEnvelope(euros("80000.00"), Money.ZERO, euros("20000.00")),
                            0L,
                            NOW);
        }

        @Test
        @DisplayName("records FundingCallOpened with the reference, the total budget, the actor and the moment")
        void recordsFundingCallOpened() {
            // when
            FundingCall call = open(TITLE, PERIOD);

            // then
            assertThat(call.pullEvents())
                    .containsExactly(
                            new FundingCallOpened(call.id(), REFERENCE, euros("80000.00"), OPENED_BY.id(), NOW));
        }

        @Test
        @DisplayName("allows a call that is open from the moment it's created")
        void allowsCallThatIsOpenFromTheMomentItsCreated() {
            // given
            SubmissionPeriod openNow = new SubmissionPeriod(NOW.minus(Duration.ofDays(1)), NOW.plusSeconds(1));

            // when
            FundingCall call = open(TITLE, openNow);

            // then
            assertThat(call.isOpenAt(NOW)).isTrue();
        }

        @Test
        @DisplayName("rejects a call that closes at the moment it's opened")
        void rejectsCallThatClosesAtTheMomentItsOpened() {
            // given
            SubmissionPeriod closingNow = new SubmissionPeriod(NOW.minus(Duration.ofDays(30)), NOW);

            // when
            Throwable thrown = catchThrowable(() -> open(TITLE, closingNow));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_ALREADY_CLOSED");
        }

        @Test
        @DisplayName("allows titles of exactly 200 characters after trimming")
        void allowsTitlesOfExactly200CharactersAfterTrimming() {
            // given
            LocalizedText longest = new LocalizedText(" " + "d".repeat(200) + " ", "f".repeat(200));

            // when
            FundingCall call = open(longest, PERIOD);

            // then
            assertThat(call.title()).isEqualTo(new LocalizedText("d".repeat(200), "f".repeat(200)));
        }

        @Test
        @DisplayName("rejects a Dutch title of 201 characters")
        void rejectsDutchTitleOf201Characters() {
            // when
            Throwable thrown =
                    catchThrowable(() -> open(new LocalizedText("d".repeat(201), "Toitures vertes 2027"), PERIOD));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_TITLE_TOO_LONG");
        }

        @Test
        @DisplayName("rejects a French title of 201 characters")
        void rejectsFrenchTitleOf201Characters() {
            // when
            Throwable thrown =
                    catchThrowable(() -> open(new LocalizedText("Groene daken 2027", "f".repeat(201)), PERIOD));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("FUNDING_CALL_TITLE_TOO_LONG");
        }

        @Test
        @DisplayName("rejects a cap per application above the total budget")
        void rejectsCapPerApplicationAboveTheTotalBudget() {
            // when
            Throwable thrown = catchThrowable(() ->
                    FundingCall.create(REFERENCE, TITLE, PERIOD, euros("20000.00"), euros("20000.01"), OPENED_BY, NOW));

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("CAP_PER_APPLICATION_EXCEEDS_BUDGET");
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
