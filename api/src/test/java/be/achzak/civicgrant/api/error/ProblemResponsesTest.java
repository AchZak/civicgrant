package be.achzak.civicgrant.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.ForbiddenException;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@ApiWebMvcTest(ProblemResponsesTest.FailureProbe.class)
@Import(ProblemResponsesTest.FailureProbe.class)
@DisplayName("Problem responses")
class ProblemResponsesTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Answering a domain exception")
    class DomainExceptions {

        @Test
        @DisplayName("answers 400 with a civicgrant problem type when a business rule is broken")
        void answers400ForABrokenRule() {
            // when
            MvcTestResult result = get(FailureProbe.BROKEN_RULE);

            // then
            assertThat(result)
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:submissionAfterClosingDate");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.title")
                    .asString()
                    .isEqualTo("Submission after closing date");
            assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(400);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.detail")
                    .asString()
                    .isEqualTo(FailureProbe.BROKEN_RULE_DETAIL);
        }

        @Test
        @DisplayName("answers 403 with a civicgrant problem type when the action is forbidden")
        void answers403ForAForbiddenAction() {
            // when
            MvcTestResult result = get(FailureProbe.FORBIDDEN);

            // then
            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:separationOfDuties");
        }

        @Test
        @DisplayName("answers 409 with a civicgrant problem type when the status doesn't allow the action")
        void answers409ForAConflict() {
            // when
            MvcTestResult result = get(FailureProbe.CONFLICT);

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:applicationNotDraft");
        }

        @Test
        @DisplayName("answers 404 with the standard resourceNotFound type, not the domain code")
        void answers404WithTheStandardType() {
            // when
            MvcTestResult result = get(FailureProbe.NOT_FOUND);

            // then
            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:resourceNotFound");
            assertThat(result).bodyText().doesNotContain("APPLICATION_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("Answering other failures")
    class OtherFailures {

        @Test
        @DisplayName("answers 400 badRequest with an issue per invalid field")
        void answers400WithAnIssuePerInvalidField() {
            // when
            MvcTestResult result = post(FailureProbe.VALIDATED_BODY, "{\"name\": \" \"}");

            // then
            assertThat(result)
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:input-validation:schemaViolation");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].in")
                    .asString()
                    .isEqualTo("body");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].name")
                    .asString()
                    .isEqualTo("/name");
        }

        @Test
        @DisplayName("answers 400 badRequest when the body isn't valid JSON")
        void answers400ForMalformedJson() {
            // when
            MvcTestResult result = post(FailureProbe.VALIDATED_BODY, "{\"name\":");

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
        }

        @Test
        @DisplayName("answers 404 resourceNotFound for an unknown path")
        void answers404ForAnUnknownPath() {
            // when
            MvcTestResult result = get("/no-such-path");

            // then
            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:resourceNotFound");
        }

        @Test
        @DisplayName("answers 500 internalServerError without leaking the exception")
        void answers500WithoutLeakingTheException() {
            // when
            MvcTestResult result = get(FailureProbe.BUG);

            // then
            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:internalServerError");
            assertThat(result).bodyText().doesNotContain(FailureProbe.BUG_DETAIL);
        }
    }

    private MvcTestResult get(String path) {
        return mvc.get().uri(path).with(jwt()).exchange();
    }

    private MvcTestResult post(String path, String json) {
        return mvc.post()
                .uri(path)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @RestController
    static class FailureProbe {

        static final String BROKEN_RULE = "/failure-probe/broken-rule";
        static final String FORBIDDEN = "/failure-probe/forbidden";
        static final String CONFLICT = "/failure-probe/conflict";
        static final String NOT_FOUND = "/failure-probe/not-found";
        static final String VALIDATED_BODY = "/failure-probe/validated-body";
        static final String BUG = "/failure-probe/bug";
        static final String BROKEN_RULE_DETAIL = "Funding call CALL-2026-01 closed at 2026-06-30T22:00:00Z";
        static final String BUG_DETAIL = "Connection to db.internal:5432 refused";

        @GetMapping(BROKEN_RULE)
        void brokenRule() {
            throw new BusinessRuleViolationException("SUBMISSION_AFTER_CLOSING_DATE", BROKEN_RULE_DETAIL);
        }

        @GetMapping(FORBIDDEN)
        void forbidden() {
            throw new ForbiddenException(
                    "SEPARATION_OF_DUTIES", "The assessor of application CG-2026-000042 can't decide on it");
        }

        @GetMapping(CONFLICT)
        void conflict() {
            throw new ConflictException("APPLICATION_NOT_DRAFT", "Application CG-2026-000042 is SUBMITTED, not DRAFT");
        }

        @GetMapping(NOT_FOUND)
        void notFound() {
            throw new NotFoundException(
                    "APPLICATION_NOT_FOUND", "Application 7b1e0c9a-3f2d-4e8b-9a6c-5d4e3f2a1b0c not found");
        }

        @PostMapping(VALIDATED_BODY)
        void validatedBody(@Valid @RequestBody ProbeBody body) {
            // Bean Validation rejects an invalid body before this method runs.
        }

        @GetMapping(BUG)
        void bug() {
            throw new IllegalStateException(BUG_DETAIL);
        }

        record ProbeBody(@NotBlank String name) {}
    }
}
