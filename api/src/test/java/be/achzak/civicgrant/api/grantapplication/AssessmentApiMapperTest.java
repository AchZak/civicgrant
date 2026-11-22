package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.AssessmentDto;
import be.achzak.civicgrant.api.generated.model.AssessmentRequestDto;
import be.achzak.civicgrant.api.generated.model.RecommendationDto;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import be.achzak.civicgrant.core.sharedkernel.domain.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assessment API mapping")
class AssessmentApiMapperTest {

    private static final Instant ASSESSED_AT = Instant.parse("2026-05-04T14:00:00Z");

    @Test
    @DisplayName("maps a recommendation to approve with its proposed amount")
    void mapsARecommendationToApproveWithItsProposedAmount() {
        // given
        AssessmentRequestDto request = new AssessmentRequestDto(true, RecommendationDto.APPROVE, "Sterk.")
                .proposedAmount(new BigDecimal("7500"));

        // when
        RecordAssessmentCommand command =
                AssessmentApiMapper.toCommand(APPLICATION_ID.value(), request, actor(Role.ASSESSOR));

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
        AssessmentRequestDto request = new AssessmentRequestDto(false, RecommendationDto.REJECT, "Geen vzw.");

        // when
        RecordAssessmentCommand command =
                AssessmentApiMapper.toCommand(APPLICATION_ID.value(), request, actor(Role.ASSESSOR));

        // then
        assertThat(command.recommendation()).isEqualTo(new Recommendation.Reject());
    }

    @Test
    @DisplayName("shows the proposed amount of a recommendation to approve")
    void showsTheProposedAmountOfARecommendationToApprove() {
        // given
        Assessment assessment = new Assessment(
                new UserId("assessor-1"), true, new Recommendation.Approve(euros("7500.00")), "Sterk.", ASSESSED_AT);

        // when
        AssessmentDto dto = AssessmentApiMapper.toDto(assessment);

        // then
        assertThat(dto)
                .isEqualTo(new AssessmentDto(
                                true, RecommendationDto.APPROVE, "Sterk.", ASSESSED_AT.atOffset(ZoneOffset.UTC))
                        .proposedAmount(new BigDecimal("7500.00")));
    }

    @Test
    @DisplayName("leaves out the amount of a recommendation to reject")
    void leavesOutTheAmountOfARecommendationToReject() {
        // given
        Assessment assessment =
                new Assessment(new UserId("assessor-1"), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT);

        // when
        AssessmentDto dto = AssessmentApiMapper.toDto(assessment);

        // then
        assertThat(dto.getRecommendation()).isEqualTo(RecommendationDto.REJECT);
        assertThat(dto.getProposedAmount()).isNull();
    }
}
