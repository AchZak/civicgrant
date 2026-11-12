package be.achzak.civicgrant.api.fundingcall;

import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.fundingcall.model.BudgetEnvelope;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsUseCase;
import be.achzak.civicgrant.domain.shared.ForbiddenException;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import be.achzak.civicgrant.domain.shared.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(FundingCallController.class)
@DisplayName("Funding call endpoints")
class FundingCallControllerTest {

    private static final Instant CLOSES_AT = Instant.parse("2026-06-30T22:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListOpenFundingCallsUseCase listOpenFundingCalls;

    @MockitoBean
    private GetFundingCallUseCase getFundingCall;

    @Test
    @DisplayName("returns the open funding calls with their dates and budget")
    void returnsTheOpenFundingCallsWithTheirDatesAndBudget() {
        // given
        FundingCall fundingCall = fundingCall();
        given(listOpenFundingCalls.list(new ListOpenFundingCallsQuery(actor(Role.APPLICANT))))
                .willReturn(List.of(fundingCall));

        // when
        MvcTestResult result =
                mvc.get().uri("/fundingCalls").with(accessToken(Role.APPLICANT)).exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].id")
                .asString()
                .isEqualTo(fundingCall.id().value().toString());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].reference")
                .asString()
                .isEqualTo("NBH-2026");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].title.fr")
                .asString()
                .isEqualTo("Projets de quartier 2026");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].closesAt")
                .asString()
                .satisfies(closesAt ->
                        assertThat(OffsetDateTime.parse(closesAt).toInstant()).isEqualTo(CLOSES_AT));
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].maxAmountPerApplication")
                .asNumber()
                .satisfies(
                        amount -> assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("15000"));
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].remainingBudget")
                .asNumber()
                .satisfies(
                        amount -> assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("37500"));
    }

    @Test
    @DisplayName("returns an empty item list when no funding call is open")
    void returnsAnEmptyItemListWhenNoFundingCallIsOpen() {
        // given
        given(listOpenFundingCalls.list(new ListOpenFundingCallsQuery(actor(Role.ASSESSOR))))
                .willReturn(List.of());

        // when
        MvcTestResult result =
                mvc.get().uri("/fundingCalls").with(accessToken(Role.ASSESSOR)).exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("{\"items\": []}");
    }

    @Test
    @DisplayName("returns a funding call by its id")
    void returnsAFundingCallByItsId() {
        // given
        FundingCall fundingCall = fundingCall();
        given(getFundingCall.get(new GetFundingCallQuery(fundingCall.id(), actor(Role.APPLICANT))))
                .willReturn(fundingCall);

        // when
        MvcTestResult result = mvc.get()
                .uri("/fundingCalls/{fundingCallId}", fundingCall.id().value())
                .with(accessToken(Role.APPLICANT))
                .exchange();

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.reference").asString().isEqualTo("NBH-2026");
    }

    @Test
    @DisplayName("returns 404 resourceNotFound for an unknown funding call")
    void returns404ForAnUnknownFundingCall() {
        // given
        FundingCallId unknown = FundingCallId.generate();
        given(getFundingCall.get(new GetFundingCallQuery(unknown, actor(Role.APPLICANT))))
                .willThrow(new NotFoundException("FUNDING_CALL_NOT_FOUND", "Funding call not found"));

        // when
        MvcTestResult result = mvc.get()
                .uri("/fundingCalls/{fundingCallId}", unknown.value())
                .with(accessToken(Role.APPLICANT))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .asString()
                .isEqualTo("urn:problem-type:belgif:resourceNotFound");
    }

    @Test
    @DisplayName("returns 403 roleRequired when the caller holds none of the accepted roles")
    void returns403RoleRequiredWhenTheCallerHoldsNoneOfTheAcceptedRoles() {
        // given
        given(listOpenFundingCalls.list(new ListOpenFundingCallsQuery(actor())))
                .willThrow(new ForbiddenException("ROLE_REQUIRED", "User has none of the roles"));

        // when
        MvcTestResult result =
                mvc.get().uri("/fundingCalls").with(accessToken()).exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .asString()
                .isEqualTo("urn:problem-type:civicgrant:roleRequired");
    }

    @Test
    @DisplayName("returns 401 noAccessToken without a token")
    void returns401WithoutAToken() {
        // when
        MvcTestResult result = mvc.get().uri("/fundingCalls").exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .asString()
                .isEqualTo("urn:problem-type:belgif:noAccessToken");
    }

    private static FundingCall fundingCall() {
        return FundingCall.restore(
                FundingCallId.generate(),
                "NBH-2026",
                new LocalizedText("Buurtprojecten 2026", "Projets de quartier 2026"),
                new SubmissionPeriod(Instant.parse("2026-03-01T08:00:00Z"), CLOSES_AT),
                new BudgetEnvelope(euros("50000.00"), euros("12500.00"), euros("15000.00")),
                0,
                Instant.parse("2026-01-10T09:00:00Z"));
    }
}
