package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.ASSESSMENT_NOT_FOUND;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.CONFLICT_OF_INTEREST;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.grantapplication.model.Assessment;
import be.achzak.civicgrant.domain.grantapplication.model.Recommendation;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetAssessmentUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.RecordAssessmentUseCase;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import be.achzak.civicgrant.domain.shared.error.ForbiddenException;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
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

@ApiWebMvcTest(AssessmentController.class)
@DisplayName("Assessment endpoints")
class AssessmentControllerTest {

    private static final String ASSESSMENT = "/api/v1/applications/" + APPLICATION_ID.value() + "/assessment";
    private static final Instant ASSESSED_AT = Instant.parse("2026-05-04T14:00:00Z");
    private static final Recommendation APPROVAL = new Recommendation.Approve(euros("7500.00"));
    private static final Assessment RECORDED =
            new Assessment(new UserId("assessor-1"), true, APPROVAL, "Sterk buurtproject.", ASSESSED_AT);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RecordAssessmentUseCase recordAssessment;

    @MockitoBean
    private GetAssessmentUseCase getAssessment;

    @Nested
    @DisplayName("Recording an assessment")
    class Recording {

        private static final RecordAssessmentCommand COMMAND = new RecordAssessmentCommand(
                APPLICATION_ID, true, APPROVAL, "Sterk buurtproject.", actor(Role.ASSESSOR));

        @Test
        @DisplayName("records the assessment and answers 201 with its absolute location and the assessment read back")
        void recordsTheAssessmentAndAnswers201() {
            // given
            given(getAssessment.get(new GetAssessmentQuery(APPLICATION_ID, actor(Role.ASSESSOR))))
                    .willReturn(RECORDED);

            // when
            MvcTestResult result = record("""
                    {"eligible": true, "recommendation": "APPROVE", "proposedAmount": 7500,
                     "comment": "Sterk buurtproject."}
                    """);

            // then
            then(recordAssessment).should().record(COMMAND);
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).headers().hasValue("Location", "http://localhost" + ASSESSMENT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.recommendation")
                    .asString()
                    .isEqualTo("APPROVE");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposedAmount")
                    .satisfies(amount ->
                            assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("7500.00"));
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.assessedAt")
                    .asString()
                    .satisfies(text ->
                            assertThat(OffsetDateTime.parse(text).toInstant()).isEqualTo(ASSESSED_AT));
        }

        @Test
        @DisplayName("answers 400 with an invalidInput issue when a recommendation to approve has no amount")
        void answers400WhenARecommendationToApproveHasNoAmount() {
            // when
            MvcTestResult result = record("""
                    {"eligible": true, "recommendation": "APPROVE", "comment": "Sterk buurtproject."}
                    """);

            // then
            assertThatBadRequestOnTheProposedAmount(result);
            assertThat(result).bodyJson().doesNotHavePath("$.issues[0].value");
            then(recordAssessment).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName(
                "answers 400 with an invalidInput issue holding the amount when a recommendation to reject has one")
        void answers400WhenARecommendationToRejectHasAnAmount() {
            // when
            MvcTestResult result = record("""
                    {"eligible": false, "recommendation": "REJECT", "proposedAmount": 7500, "comment": "Geen vzw."}
                    """);

            // then
            assertThatBadRequestOnTheProposedAmount(result);
            assertThat(result).bodyJson().extractingPath("$.issues[0].value").isEqualTo(7500);
            then(recordAssessment).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("answers 403 conflictOfInterest when the assessor is the applicant")
        void answers403WhenTheAssessorIsTheApplicant() {
            // given
            willThrow(new ForbiddenException(CONFLICT_OF_INTEREST, "The applicant can't assess"))
                    .given(recordAssessment)
                    .record(COMMAND);

            // when
            MvcTestResult result = record("""
                    {"eligible": true, "recommendation": "APPROVE", "proposedAmount": 7500.00,
                     "comment": "Sterk buurtproject."}
                    """);

            // then
            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:conflictOfInterest");
        }

        private MvcTestResult record(String body) {
            return mvc.post()
                    .uri(ASSESSMENT)
                    .with(accessToken(Role.ASSESSOR))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();
        }

        private void assertThatBadRequestOnTheProposedAmount(MvcTestResult result) {
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
                    .isEqualTo("/proposedAmount");
        }
    }

    @Nested
    @DisplayName("Getting the assessment")
    class Getting {

        @Test
        @DisplayName("returns a recommendation to reject without an amount")
        void returnsARecommendationToRejectWithoutAnAmount() {
            // given
            given(getAssessment.get(new GetAssessmentQuery(APPLICATION_ID, actor(Role.DECISION_MAKER))))
                    .willReturn(new Assessment(
                            new UserId("assessor-1"), false, new Recommendation.Reject(), "Geen vzw.", ASSESSED_AT));

            // when
            MvcTestResult result = mvc.get()
                    .uri(ASSESSMENT)
                    .with(accessToken(Role.DECISION_MAKER))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.eligible").isEqualTo(false);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.recommendation")
                    .asString()
                    .isEqualTo("REJECT");
            assertThat(result).bodyJson().extractingPath("$.comment").asString().isEqualTo("Geen vzw.");
            assertThat(result).bodyJson().doesNotHavePath("$.proposedAmount");
        }

        @Test
        @DisplayName("returns 404 resourceNotFound until the application is assessed")
        void returns404UntilTheApplicationIsAssessed() {
            // given
            given(getAssessment.get(new GetAssessmentQuery(APPLICATION_ID, actor(Role.ASSESSOR))))
                    .willThrow(new NotFoundException(ASSESSMENT_NOT_FOUND, "Not assessed yet"));

            // when
            MvcTestResult result =
                    mvc.get().uri(ASSESSMENT).with(accessToken(Role.ASSESSOR)).exchange();

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
