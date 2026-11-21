package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.api.generated.model.DecisionDto;
import be.achzak.civicgrant.api.generated.model.DecisionOutcomeDto;
import be.achzak.civicgrant.api.generated.model.DecisionRequestDto;
import be.achzak.civicgrant.api.generated.model.RejectionGroundDto;
import be.achzak.civicgrant.domain.grantapplication.model.Decision;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.grantapplication.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Decision API mapping")
class DecisionApiMapperTest {

    private static final Instant DECIDED_AT = Instant.parse("2026-05-18T09:30:00Z");
    private static final UserId DECISION_MAKER = new UserId("decision-maker-1");

    @Test
    @DisplayName("maps an approval to the approve command with its granted amount")
    void mapsAnApprovalToTheApproveCommand() {
        // given
        DecisionRequestDto request = new DecisionRequestDto(DecisionOutcomeDto.APPROVED, "Versterkt de buurt.")
                .grantedAmount(new BigDecimal("7000"));

        // when
        ApproveApplicationCommand command =
                DecisionApiMapper.toApproveCommand(APPLICATION_ID.value(), request, actor(Role.DECISION_MAKER));

        // then
        assertThat(command)
                .isEqualTo(new ApproveApplicationCommand(
                        APPLICATION_ID, euros("7000.00"), "Versterkt de buurt.", actor(Role.DECISION_MAKER)));
    }

    @Test
    @DisplayName("maps a rejection to the reject command with its ground")
    void mapsARejectionToTheRejectCommand() {
        // given
        DecisionRequestDto request = new DecisionRequestDto(DecisionOutcomeDto.REJECTED, "Geen vzw.")
                .rejectionGround(RejectionGroundDto.INELIGIBLE);

        // when
        RejectApplicationCommand command =
                DecisionApiMapper.toRejectCommand(APPLICATION_ID.value(), request, actor(Role.DECISION_MAKER));

        // then
        assertThat(command)
                .isEqualTo(new RejectApplicationCommand(
                        APPLICATION_ID, RejectionGround.INELIGIBLE, "Geen vzw.", actor(Role.DECISION_MAKER)));
    }

    @Test
    @DisplayName("shows the granted amount of an approval")
    void showsTheGrantedAmountOfAnApproval() {
        // given
        Decision approval = new Decision.Approval(euros("7000.00"), DECISION_MAKER, "Versterkt de buurt.", DECIDED_AT);

        // when
        DecisionDto dto = DecisionApiMapper.toDto(approval);

        // then
        assertThat(dto)
                .isEqualTo(new DecisionDto(
                                DecisionOutcomeDto.APPROVED, "Versterkt de buurt.", DECIDED_AT.atOffset(ZoneOffset.UTC))
                        .grantedAmount(new BigDecimal("7000.00")));
    }

    @Test
    @DisplayName("shows the ground of a rejection, without an amount")
    void showsTheGroundOfARejection() {
        // given
        Decision rejection =
                new Decision.Rejection(RejectionGround.INCOMPLETE, DECISION_MAKER, "Geen begroting.", DECIDED_AT);

        // when
        DecisionDto dto = DecisionApiMapper.toDto(rejection);

        // then
        assertThat(dto)
                .isEqualTo(new DecisionDto(
                                DecisionOutcomeDto.REJECTED, "Geen begroting.", DECIDED_AT.atOffset(ZoneOffset.UTC))
                        .rejectionGround(RejectionGroundDto.INCOMPLETE));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RejectionGroundDto.class)
    @DisplayName("maps every rejection ground of the contract to the domain")
    void mapsEveryRejectionGroundOfTheContractToTheDomain(RejectionGroundDto ground) {
        // when
        RejectionGround mapped = DecisionApiMapper.toDomain(ground);

        // then
        assertThat(mapped.name()).isEqualTo(ground.name());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(RejectionGround.class)
    @DisplayName("maps every rejection ground of the domain to the contract")
    void mapsEveryRejectionGroundOfTheDomainToTheContract(RejectionGround ground) {
        // when
        String mapped = DecisionApiMapper.toDto(ground).name();

        // then
        assertThat(mapped).isEqualTo(ground.name());
    }
}
