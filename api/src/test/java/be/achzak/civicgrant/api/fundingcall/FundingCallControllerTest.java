package be.achzak.civicgrant.api.fundingcall;

import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_NOT_FOUND;
import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode.FUNDING_CALL_REFERENCE_TAKEN;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static be.achzak.civicgrant.domain.shared.SharedErrorCode.ROLE_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.fundingcall.model.BudgetEnvelope;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.model.SubmissionPeriod;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.GetFundingCallUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.domain.fundingcall.port.in.ListOpenFundingCallsUseCase;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.domain.fundingcall.port.in.OpenFundingCallUseCase;
import be.achzak.civicgrant.domain.shared.LocalizedText;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.error.ConflictException;
import be.achzak.civicgrant.domain.shared.error.ForbiddenException;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @MockitoBean
    private OpenFundingCallUseCase openFundingCall;

    @Nested
    @DisplayName("Opening a funding call")
    class Opening {

        private static final Instant NEW_CALL_OPENS_AT = Instant.parse("2026-12-01T08:00:00Z");
        private static final Instant NEW_CALL_CLOSES_AT = Instant.parse("2027-03-31T22:00:00Z");

        private final OpenFundingCallCommand command = new OpenFundingCallCommand(
                "GRN-2027",
                new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                new SubmissionPeriod(NEW_CALL_OPENS_AT, NEW_CALL_CLOSES_AT),
                euros("80000.00"),
                euros("20000.00"),
                actor(Role.PROGRAMME_MANAGER));

        @Test
        @DisplayName("opens a call and answers 201 with its location and the stored call")
        void opensACallAndAnswers201WithItsLocationAndTheStoredCall() {
            // given
            FundingCall opened = FundingCall.restore(
                    FundingCallId.generate(),
                    "GRN-2027",
                    command.title(),
                    command.submissionPeriod(),
                    BudgetEnvelope.unspent(euros("80000.00"), euros("20000.00")),
                    0,
                    Instant.parse("2026-11-21T10:00:00Z"));
            given(openFundingCall.open(command)).willReturn(opened.id());
            given(getFundingCall.get(new GetFundingCallQuery(opened.id(), actor(Role.PROGRAMME_MANAGER))))
                    .willReturn(opened);

            // when
            MvcTestResult result = open("GRN-2027", NEW_CALL_CLOSES_AT);

            // then
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result)
                    .headers()
                    .hasValue(
                            "Location",
                            "http://localhost/api/v1/fundingCalls/"
                                    + opened.id().value());
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.reference")
                    .asString()
                    .isEqualTo("GRN-2027");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.remainingBudget")
                    .asNumber()
                    .satisfies(amount ->
                            assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("80000"));
        }

        @Test
        @DisplayName("returns 400 submissionPeriodClosesBeforeOpening without calling the use case")
        void returns400WhenTheCallClosesBeforeItOpens() {
            // when
            MvcTestResult result = open("GRN-2027", NEW_CALL_OPENS_AT.minusSeconds(1));

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:submissionPeriodClosesBeforeOpening");
            then(openFundingCall).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 400 badRequest for a reference in lower case, which the contract refuses")
        void returns400BadRequestForAReferenceInLowerCase() {
            // when
            MvcTestResult result = open("grn-2027", NEW_CALL_CLOSES_AT);

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
            then(openFundingCall).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 409 fundingCallReferenceTaken for a reference already in use")
        void returns409ForAReferenceAlreadyInUse() {
            // given
            given(openFundingCall.open(command))
                    .willThrow(new ConflictException(
                            FUNDING_CALL_REFERENCE_TAKEN, "Funding call reference GRN-2027 is already taken"));

            // when
            MvcTestResult result = open("GRN-2027", NEW_CALL_CLOSES_AT);

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:fundingCallReferenceTaken");
        }

        private MvcTestResult open(String reference, Instant closesAt) {
            return mvc.post()
                    .uri("/api/v1/fundingCalls")
                    .with(accessToken(Role.PROGRAMME_MANAGER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "reference": "%s",
                              "title": {"nl": "Groene daken 2027", "fr": "Toitures vertes 2027"},
                              "opensAt": "%s",
                              "closesAt": "%s",
                              "totalBudget": 80000.00,
                              "maxAmountPerApplication": 20000.00
                            }
                            """.formatted(reference, NEW_CALL_OPENS_AT, closesAt))
                    .exchange();
        }
    }

    @Test
    @DisplayName("returns the open funding calls with their dates and budget")
    void returnsTheOpenFundingCallsWithTheirDatesAndBudget() {
        // given
        FundingCall fundingCall = fundingCall();
        given(listOpenFundingCalls.list(new ListOpenFundingCallsQuery(actor(Role.APPLICANT))))
                .willReturn(List.of(fundingCall));

        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/fundingCalls")
                .with(accessToken(Role.APPLICANT))
                .exchange();

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
        MvcTestResult result = mvc.get()
                .uri("/api/v1/fundingCalls")
                .with(accessToken(Role.ASSESSOR))
                .exchange();

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
                .uri("/api/v1/fundingCalls/{fundingCallId}", fundingCall.id().value())
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
                .willThrow(new NotFoundException(FUNDING_CALL_NOT_FOUND, "Funding call not found"));

        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/fundingCalls/{fundingCallId}", unknown.value())
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
                .willThrow(new ForbiddenException(ROLE_REQUIRED, "User has none of the roles"));

        // when
        MvcTestResult result =
                mvc.get().uri("/api/v1/fundingCalls").with(accessToken()).exchange();

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
        MvcTestResult result = mvc.get().uri("/api/v1/fundingCalls").exchange();

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
