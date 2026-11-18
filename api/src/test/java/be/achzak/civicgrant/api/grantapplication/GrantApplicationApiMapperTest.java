package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aDraft;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aProposal;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.AssessmentRequest;
import be.achzak.civicgrant.api.generated.model.DecisionOutcome;
import be.achzak.civicgrant.api.generated.model.DecisionRequest;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Assessment;
import be.achzak.civicgrant.domain.grantapplication.model.Decision;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Grant application API mapping")
class GrantApplicationApiMapperTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(be.achzak.civicgrant.api.generated.model.Language.class)
    @DisplayName("maps every language of the contract to the domain")
    void mapsEveryLanguageOfTheContractToTheDomain(be.achzak.civicgrant.api.generated.model.Language language) {
        // given
        be.achzak.civicgrant.api.generated.model.ProjectProposal proposal =
                new be.achzak.civicgrant.api.generated.model.ProjectProposal(
                        "Samen tuinieren", "Een moestuin.", new BigDecimal("8000"), new BigDecimal("12000"), language);

        // when
        ProjectProposal mapped = GrantApplicationApiMapper.toDomain(proposal);

        // then
        assertThat(mapped.language().name()).isEqualTo(language.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Language.class)
    @DisplayName("maps every language of the domain to the contract")
    void mapsEveryLanguageOfTheDomainToTheContract(Language language) {
        // when
        String mapped = GrantApplicationApiMapper.toResource(aDraft(aProposal(language)))
                .getProposal()
                .getLanguage()
                .name();

        // then
        assertThat(mapped).isEqualTo(language.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(be.achzak.civicgrant.api.generated.model.ApplicationStatus.class)
    @DisplayName("maps every status of the contract to the domain")
    void mapsEveryStatusOfTheContractToTheDomain(be.achzak.civicgrant.api.generated.model.ApplicationStatus status) {
        // when
        SearchApplicationsQuery query = GrantApplicationApiMapper.toQuery(status, null, 1, 20, actor(Role.ASSESSOR));

        // then
        assertThat(query.statuses()).extracting(Enum::name).containsExactly(status.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ApplicationStatus.class)
    @DisplayName("maps every status of the domain to the contract")
    void mapsEveryStatusOfTheDomainToTheContract(ApplicationStatus status) {
        // when
        String mapped = GrantApplicationApiMapper.toResource(status).name();

        // then
        assertThat(mapped).isEqualTo(status.name());
    }

    @Nested
    @DisplayName("Assessments")
    class Assessments {

        private static final Instant ASSESSED_AT = Instant.parse("2026-05-04T14:00:00Z");

        @Test
        @DisplayName("maps a recommendation to approve with its proposed amount")
        void mapsARecommendationToApproveWithItsProposedAmount() {
            // given
            AssessmentRequest request = new AssessmentRequest(
                            true, be.achzak.civicgrant.api.generated.model.Recommendation.APPROVE, "Sterk.")
                    .proposedAmount(new BigDecimal("7500"));

            // when
            RecordAssessmentCommand command =
                    GrantApplicationApiMapper.toCommand(APPLICATION_ID.value(), request, actor(Role.ASSESSOR));

            // then
            assertThat(command)
                    .isEqualTo(new RecordAssessmentCommand(
                            APPLICATION_ID,
                            true,
                            new Recommendation.Approve(euros("7500.00")),
                            "Sterk.",
                            actor(Role.ASSESSOR)));
        }

        @Test
        @DisplayName("maps a recommendation to reject without an amount")
        void mapsARecommendationToRejectWithoutAnAmount() {
            // given
            AssessmentRequest request = new AssessmentRequest(
                    false, be.achzak.civicgrant.api.generated.model.Recommendation.REJECT, "Geen vzw.");

            // when
            RecordAssessmentCommand command =
                    GrantApplicationApiMapper.toCommand(APPLICATION_ID.value(), request, actor(Role.ASSESSOR));

            // then
            assertThat(command.recommendation()).isEqualTo(new Recommendation.Reject());
        }

        @Test
        @DisplayName("shows the proposed amount of a recommendation to approve")
        void showsTheProposedAmountOfARecommendationToApprove() {
            // given
            Assessment assessment = new Assessment(
                    new UserId("assessor-1"),
                    true,
                    new Recommendation.Approve(euros("7500.00")),
                    "Sterk.",
                    ASSESSED_AT);

            // when
            be.achzak.civicgrant.api.generated.model.Assessment resource =
                    GrantApplicationApiMapper.toResource(assessment);

            // then
            assertThat(resource)
                    .isEqualTo(new be.achzak.civicgrant.api.generated.model.Assessment(
                                    true,
                                    be.achzak.civicgrant.api.generated.model.Recommendation.APPROVE,
                                    "Sterk.",
                                    ASSESSED_AT.atOffset(ZoneOffset.UTC))
                            .proposedAmount(new BigDecimal("7500.00")));
        }

        @Test
        @DisplayName("leaves out the amount of a recommendation to reject")
        void leavesOutTheAmountOfARecommendationToReject() {
            // given
            Assessment assessment = new Assessment(
                    new UserId("assessor-1"), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT);

            // when
            be.achzak.civicgrant.api.generated.model.Assessment resource =
                    GrantApplicationApiMapper.toResource(assessment);

            // then
            assertThat(resource.getRecommendation())
                    .isEqualTo(be.achzak.civicgrant.api.generated.model.Recommendation.REJECT);
            assertThat(resource.getProposedAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("Decisions")
    class Decisions {

        private static final Instant DECIDED_AT = Instant.parse("2026-05-18T09:30:00Z");
        private static final UserId DECISION_MAKER = new UserId("decision-maker-1");

        @Test
        @DisplayName("maps an approval to the approve command with its granted amount")
        void mapsAnApprovalToTheApproveCommand() {
            // given
            DecisionRequest request = new DecisionRequest(DecisionOutcome.APPROVED, "Versterkt de buurt.")
                    .grantedAmount(new BigDecimal("7000"));

            // when
            ApproveApplicationCommand command = GrantApplicationApiMapper.toApproveCommand(
                    APPLICATION_ID.value(), request, actor(Role.DECISION_MAKER));

            // then
            assertThat(command)
                    .isEqualTo(new ApproveApplicationCommand(
                            APPLICATION_ID, euros("7000.00"), "Versterkt de buurt.", actor(Role.DECISION_MAKER)));
        }

        @Test
        @DisplayName("maps a rejection to the reject command with its ground")
        void mapsARejectionToTheRejectCommand() {
            // given
            DecisionRequest request = new DecisionRequest(DecisionOutcome.REJECTED, "Geen vzw.")
                    .rejectionGround(be.achzak.civicgrant.api.generated.model.RejectionGround.INELIGIBLE);

            // when
            RejectApplicationCommand command = GrantApplicationApiMapper.toRejectCommand(
                    APPLICATION_ID.value(), request, actor(Role.DECISION_MAKER));

            // then
            assertThat(command)
                    .isEqualTo(new RejectApplicationCommand(
                            APPLICATION_ID, RejectionGround.INELIGIBLE, "Geen vzw.", actor(Role.DECISION_MAKER)));
        }

        @Test
        @DisplayName("shows the granted amount of an approval")
        void showsTheGrantedAmountOfAnApproval() {
            // given
            Decision approval =
                    new Decision.Approval(euros("7000.00"), DECISION_MAKER, "Versterkt de buurt.", DECIDED_AT);

            // when
            be.achzak.civicgrant.api.generated.model.Decision resource = GrantApplicationApiMapper.toResource(approval);

            // then
            assertThat(resource)
                    .isEqualTo(new be.achzak.civicgrant.api.generated.model.Decision(
                                    DecisionOutcome.APPROVED,
                                    "Versterkt de buurt.",
                                    DECIDED_AT.atOffset(ZoneOffset.UTC))
                            .grantedAmount(new BigDecimal("7000.00")));
        }

        @Test
        @DisplayName("shows the ground of a rejection, without an amount")
        void showsTheGroundOfARejection() {
            // given
            Decision rejection =
                    new Decision.Rejection(RejectionGround.INCOMPLETE, DECISION_MAKER, "Geen begroting.", DECIDED_AT);

            // when
            be.achzak.civicgrant.api.generated.model.Decision resource =
                    GrantApplicationApiMapper.toResource(rejection);

            // then
            assertThat(resource)
                    .isEqualTo(new be.achzak.civicgrant.api.generated.model.Decision(
                                    DecisionOutcome.REJECTED, "Geen begroting.", DECIDED_AT.atOffset(ZoneOffset.UTC))
                            .rejectionGround(be.achzak.civicgrant.api.generated.model.RejectionGround.INCOMPLETE));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(be.achzak.civicgrant.api.generated.model.RejectionGround.class)
        @DisplayName("maps every rejection ground of the contract to the domain")
        void mapsEveryRejectionGroundOfTheContractToTheDomain(
                be.achzak.civicgrant.api.generated.model.RejectionGround ground) {
            // when
            RejectionGround mapped = GrantApplicationApiMapper.toDomain(ground);

            // then
            assertThat(mapped.name()).isEqualTo(ground.name());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(RejectionGround.class)
        @DisplayName("maps every rejection ground of the domain to the contract")
        void mapsEveryRejectionGroundOfTheDomainToTheContract(RejectionGround ground) {
            // when
            String mapped = GrantApplicationApiMapper.toResource(ground).name();

            // then
            assertThat(mapped).isEqualTo(ground.name());
        }
    }
}
