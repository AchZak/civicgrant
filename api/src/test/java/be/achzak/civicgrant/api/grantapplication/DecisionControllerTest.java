package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.DECIDED_AT;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aRejectedApplication;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.anApprovedApplication;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.anAssessedApplication;
import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.core.common.domain.error.CommonErrorCode.CONCURRENT_UPDATE;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode.SEPARATION_OF_DUTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.core.common.domain.error.ConflictException;
import be.achzak.civicgrant.core.common.domain.error.ForbiddenException;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationUseCase;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(DecisionController.class)
@DisplayName("Decision endpoints")
class DecisionControllerTest {

    private static final String DECISION = "/api/v1/applications/" + APPLICATION_ID.value() + "/decision";
    private static final String APPROVAL = """
            {"outcome": "APPROVED", "grantedAmount": 7000, "motivation": "Het project versterkt de buurt."}
            """;
    private static final String REJECTION = """
            {"outcome": "REJECTED", "rejectionGround": "INSUFFICIENT_QUALITY",
             "motivation": "Het projectplan is onvoldoende uitgewerkt."}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ApproveApplicationUseCase approveApplication;

    @MockitoBean
    private RejectApplicationUseCase rejectApplication;

    @MockitoBean
    private GetApplicationUseCase getApplication;

    @Nested
    @DisplayName("Deciding on an application")
    class Deciding {

        private static final ApproveApplicationCommand APPROVE = new ApproveApplicationCommand(
                APPLICATION_ID, euros("7000.00"), "Het project versterkt de buurt.", actor(Role.DECISION_MAKER));

        @Test
        @DisplayName("approves and answers 201 with its absolute location and the approval read back")
        void approvesAndAnswers201WithTheApproval() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.DECISION_MAKER))))
                    .willReturn(anApprovedApplication());

            // when
            MvcTestResult result = decide(APPROVAL);

            // then
            then(approveApplication).should().approve(APPROVE);
            then(rejectApplication).shouldHaveNoInteractions();
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).headers().hasValue("Location", "http://localhost" + DECISION);
            assertThat(result).bodyJson().extractingPath("$.outcome").asString().isEqualTo("APPROVED");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.grantedAmount")
                    .satisfies(amount ->
                            assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("7000.00"));
            assertThat(result).bodyJson().doesNotHavePath("$.rejectionGround");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.decidedAt")
                    .asString()
                    .satisfies(text ->
                            assertThat(OffsetDateTime.parse(text).toInstant()).isEqualTo(DECIDED_AT));
        }

        @Test
        @DisplayName("rejects and answers 201 with the rejection read back, without an amount")
        void rejectsAndAnswers201WithTheRejection() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.DECISION_MAKER))))
                    .willReturn(aRejectedApplication());

            // when
            MvcTestResult result = decide(REJECTION);

            // then
            then(rejectApplication)
                    .should()
                    .reject(new RejectApplicationCommand(
                            APPLICATION_ID,
                            RejectionGround.INSUFFICIENT_QUALITY,
                            "Het projectplan is onvoldoende uitgewerkt.",
                            actor(Role.DECISION_MAKER)));
            then(approveApplication).shouldHaveNoInteractions();
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.outcome").asString().isEqualTo("REJECTED");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.rejectionGround")
                    .asString()
                    .isEqualTo("INSUFFICIENT_QUALITY");
            assertThat(result).bodyJson().doesNotHavePath("$.grantedAmount");
        }

        @Test
        @DisplayName("answers 400 with an invalidInput issue when an approval has no granted amount")
        void answers400WhenAnApprovalHasNoGrantedAmount() {
            // when
            MvcTestResult result = decide("""
                    {"outcome": "APPROVED", "motivation": "Het project versterkt de buurt."}
                    """);

            // then
            assertThatBadRequestOn(result, "/grantedAmount");
            assertThat(result).bodyJson().doesNotHavePath("$.issues[0].value");
            then(approveApplication).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("answers 400 with an invalidInput issue holding the ground when an approval has one")
        void answers400WhenAnApprovalHasARejectionGround() {
            // when
            MvcTestResult result = decide("""
                    {"outcome": "APPROVED", "grantedAmount": 7000, "rejectionGround": "INELIGIBLE",
                     "motivation": "Het project versterkt de buurt."}
                    """);

            // then
            assertThatBadRequestOn(result, "/rejectionGround");
            assertThat(result).bodyJson().extractingPath("$.issues[0].value").isEqualTo("INELIGIBLE");
            then(approveApplication).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("answers 400 with an invalidInput issue when a rejection has no ground")
        void answers400WhenARejectionHasNoGround() {
            // when
            MvcTestResult result = decide("""
                    {"outcome": "REJECTED", "motivation": "Het projectplan is onvoldoende uitgewerkt."}
                    """);

            // then
            assertThatBadRequestOn(result, "/rejectionGround");
            assertThat(result).bodyJson().doesNotHavePath("$.issues[0].value");
            then(rejectApplication).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("answers 400 with an invalidInput issue holding the amount when a rejection has one")
        void answers400WhenARejectionHasAGrantedAmount() {
            // when
            MvcTestResult result = decide("""
                    {"outcome": "REJECTED", "rejectionGround": "INELIGIBLE", "grantedAmount": 7000,
                     "motivation": "Het projectplan is onvoldoende uitgewerkt."}
                    """);

            // then
            assertThatBadRequestOn(result, "/grantedAmount");
            assertThat(result).bodyJson().extractingPath("$.issues[0].value").isEqualTo(7000);
            then(rejectApplication).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("answers 403 separationOfDuties when the decision maker assessed the application")
        void answers403WhenTheDecisionMakerAssessedTheApplication() {
            // given
            willThrow(new ForbiddenException(SEPARATION_OF_DUTIES, "The assessor can't decide"))
                    .given(approveApplication)
                    .approve(APPROVE);

            // when
            MvcTestResult result = decide(APPROVAL);

            // then
            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:separationOfDuties");
        }

        @Test
        @DisplayName("answers 409 concurrentUpdate when another decision changed the call's budget first")
        void answers409WhenAnotherDecisionChangedTheBudgetFirst() {
            // given
            willThrow(new ConflictException(CONCURRENT_UPDATE, "Funding call NBH-2026 was changed meanwhile"))
                    .given(approveApplication)
                    .approve(APPROVE);

            // when
            MvcTestResult result = decide(APPROVAL);

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:concurrentUpdate");
        }

        private MvcTestResult decide(String body) {
            return mvc.post()
                    .uri(DECISION)
                    .with(accessToken(Role.DECISION_MAKER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();
        }

        private void assertThatBadRequestOn(MvcTestResult result, String pointer) {
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].name")
                    .asString()
                    .isEqualTo(pointer);
        }
    }

    @Nested
    @DisplayName("Getting the decision")
    class Getting {

        @Test
        @DisplayName("returns the decision to the applicant")
        void returnsTheDecisionToTheApplicant() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willReturn(aRejectedApplication());

            // when
            MvcTestResult result =
                    mvc.get().uri(DECISION).with(accessToken(Role.APPLICANT)).exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.outcome").asString().isEqualTo("REJECTED");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.motivation")
                    .asString()
                    .isEqualTo("Het projectplan is onvoldoende uitgewerkt.");
        }

        @Test
        @DisplayName("returns 404 resourceNotFound until the application is decided")
        void returns404UntilTheApplicationIsDecided() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.ASSESSOR))))
                    .willReturn(anAssessedApplication());

            // when
            MvcTestResult result =
                    mvc.get().uri(DECISION).with(accessToken(Role.ASSESSOR)).exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:resourceNotFound");
        }
    }
}
