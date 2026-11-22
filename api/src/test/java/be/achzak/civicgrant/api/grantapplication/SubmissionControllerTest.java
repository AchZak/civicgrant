package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.SUBMITTED_AT;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aDraft;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aProposal;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aSubmittedApplication;
import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.APPLICATION_NOT_DRAFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationUseCase;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.error.ConflictException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(SubmissionController.class)
@DisplayName("Submission endpoints")
class SubmissionControllerTest {

    private static final String SUBMISSION = "/api/v1/applications/" + APPLICATION_ID.value() + "/submission";
    private static final SubmitApplicationCommand COMMAND =
            new SubmitApplicationCommand(APPLICATION_ID, actor(Role.APPLICANT));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SubmitApplicationUseCase submitApplication;

    @MockitoBean
    private GetApplicationUseCase getApplication;

    @Nested
    @DisplayName("Submitting a draft")
    class Submitting {

        @Test
        @DisplayName("submits the draft and answers 201 with the submission's absolute location and moment")
        void submitsTheDraftAndAnswers201WithTheSubmission() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willReturn(aSubmittedApplication());

            // when
            MvcTestResult result =
                    mvc.post().uri(SUBMISSION).with(accessToken(Role.APPLICANT)).exchange();

            // then
            then(submitApplication).should().submit(COMMAND);
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).headers().hasValue("Location", "http://localhost" + SUBMISSION);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.submittedAt")
                    .asString()
                    .satisfies(text ->
                            assertThat(OffsetDateTime.parse(text).toInstant()).isEqualTo(SUBMITTED_AT));
        }

        @Test
        @DisplayName("answers 409 applicationNotDraft when the application was already submitted")
        void answers409WhenAlreadySubmitted() {
            // given
            willThrow(new ConflictException(APPLICATION_NOT_DRAFT, "Application is SUBMITTED, not DRAFT"))
                    .given(submitApplication)
                    .submit(COMMAND);

            // when
            MvcTestResult result =
                    mvc.post().uri(SUBMISSION).with(accessToken(Role.APPLICANT)).exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:applicationNotDraft");
        }
    }

    @Nested
    @DisplayName("Getting the submission")
    class Getting {

        @Test
        @DisplayName("returns the moment of submission")
        void returnsTheMomentOfSubmission() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.ASSESSOR))))
                    .willReturn(aSubmittedApplication());

            // when
            MvcTestResult result =
                    mvc.get().uri(SUBMISSION).with(accessToken(Role.ASSESSOR)).exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.submittedAt")
                    .asString()
                    .satisfies(text ->
                            assertThat(OffsetDateTime.parse(text).toInstant()).isEqualTo(SUBMITTED_AT));
        }

        @Test
        @DisplayName("returns 404 resourceNotFound while the application is a draft")
        void returns404WhileTheApplicationIsADraft() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willReturn(aDraft(aProposal(Language.NL)));

            // when
            MvcTestResult result =
                    mvc.get().uri(SUBMISSION).with(accessToken(Role.APPLICANT)).exchange();

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
