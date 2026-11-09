package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Project proposal")
class ProjectProposalTest {

    @Nested
    @DisplayName("Requested amount")
    class RequestedAmount {

        @Test
        @DisplayName("may equal the total project budget")
        void mayEqualTheTotalProjectBudget() {
            // given
            ProjectProposalBuilder proposal =
                    aProjectProposal().withRequestedAmount(euros("12000.00")).withTotalProjectBudget(euros("12000.00"));

            // when
            ProjectProposal built = proposal.build();

            // then
            assertThat(built.requestedAmount()).isEqualTo(euros("12000.00"));
        }

        @Test
        @DisplayName("rejects a requested amount one cent above the total project budget")
        void rejectsRequestedAmountOneCentAboveTheTotalProjectBudget() {
            // given
            ProjectProposalBuilder proposal =
                    aProjectProposal().withRequestedAmount(euros("12000.01")).withTotalProjectBudget(euros("12000.00"));

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown)
                    .isBusinessRuleViolation()
                    .hasCode("REQUESTED_AMOUNT_EXCEEDS_PROJECT_BUDGET");
        }

        @Test
        @DisplayName("rejects a requested amount of zero")
        void rejectsRequestedAmountOfZero() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withRequestedAmount(euros("0.00"));

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("REQUESTED_AMOUNT_NOT_POSITIVE");
        }
    }

    @Nested
    @DisplayName("Title")
    class Title {

        @Test
        @DisplayName("strips whitespace around the title")
        void stripsWhitespaceAroundTheTitle() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withTitle("  Moestuin voor de buurt ");

            // when
            ProjectProposal built = proposal.build();

            // then
            assertThat(built.title()).isEqualTo("Moestuin voor de buurt");
        }

        @Test
        @DisplayName("rejects a blank title")
        void rejectsBlankTitle() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withTitle("   ");

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROJECT_TITLE_REQUIRED");
        }

        @Test
        @DisplayName("accepts a title of exactly 200 characters")
        void acceptsTitleOfExactly200Characters() {
            // given
            String longestTitle = "a".repeat(200);

            // when
            ProjectProposal built = aProjectProposal().withTitle(longestTitle).build();

            // then
            assertThat(built.title()).isEqualTo(longestTitle);
        }

        @Test
        @DisplayName("rejects a title longer than 200 characters")
        void rejectsTitleLongerThan200Characters() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withTitle("a".repeat(201));

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROJECT_TITLE_TOO_LONG");
        }
    }

    @Nested
    @DisplayName("Description")
    class Description {

        @Test
        @DisplayName("rejects a blank description")
        void rejectsBlankDescription() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withDescription("");

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROJECT_DESCRIPTION_REQUIRED");
        }

        @Test
        @DisplayName("accepts a description of exactly 10,000 characters")
        void acceptsDescriptionOfExactly10000Characters() {
            // given
            String longestDescription = "a".repeat(10_000);

            // when
            ProjectProposal built =
                    aProjectProposal().withDescription(longestDescription).build();

            // then
            assertThat(built.description()).isEqualTo(longestDescription);
        }

        @Test
        @DisplayName("rejects a description longer than 10,000 characters")
        void rejectsDescriptionLongerThan10000Characters() {
            // given
            ProjectProposalBuilder proposal = aProjectProposal().withDescription("a".repeat(10_001));

            // when
            Throwable thrown = catchThrowable(proposal::build);

            // then
            assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("PROJECT_DESCRIPTION_TOO_LONG");
        }
    }
}
