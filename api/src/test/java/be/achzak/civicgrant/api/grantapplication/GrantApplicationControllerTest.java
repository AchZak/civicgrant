package be.achzak.civicgrant.api.grantapplication;

import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.APPLICATION_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.FUNDING_CALL_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.ORGANISATION_ID;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.SUBMITTED_AT;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aDraft;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aProposal;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aSubmittedApplication;
import static be.achzak.civicgrant.api.grantapplication.GrantApplicationFixtures.aSummary;
import static be.achzak.civicgrant.api.security.AccessTokens.accessToken;
import static be.achzak.civicgrant.api.security.AccessTokens.actor;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.APPLICATION_NOT_DRAFT;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationErrorCode.APPLICATION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import be.achzak.civicgrant.api.security.ApiWebMvcTest;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallErrorCode;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationPage;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Language;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.in.EditApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.GetApplicationUseCase;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.domain.grantapplication.port.in.SearchApplicationsUseCase;
import be.achzak.civicgrant.domain.organisation.model.OrganisationErrorCode;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.error.ConflictException;
import be.achzak.civicgrant.domain.shared.error.ErrorCode;
import be.achzak.civicgrant.domain.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiWebMvcTest(GrantApplicationController.class)
@DisplayName("Grant application endpoints")
class GrantApplicationControllerTest {

    private static final String PROPOSAL_JSON = """
            {"title": "Samen tuinieren in de wijk",
             "description": "Een gedeelde moestuin voor de buurtbewoners.",
             "requestedAmount": 8000.00, "totalProjectBudget": 12000.00, "language": "NL"}
            """;
    private static final String APPLICATIONS = "/api/v1/applications";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateApplicationUseCase createApplication;

    @MockitoBean
    private EditApplicationUseCase editApplication;

    @MockitoBean
    private GetApplicationUseCase getApplication;

    @MockitoBean
    private SearchApplicationsUseCase searchApplications;

    @Nested
    @DisplayName("Creating an application")
    class Creating {

        private static final CreateApplicationCommand COMMAND = new CreateApplicationCommand(
                FUNDING_CALL_ID, ORGANISATION_ID, aProposal(Language.NL), actor(Role.APPLICANT));

        @Test
        @DisplayName("creates a draft and answers 201 with its absolute location and the stored application")
        void createsADraftAndAnswers201WithItsLocationAndTheStoredApplication() {
            // given
            given(createApplication.create(COMMAND)).willReturn(APPLICATION_ID);
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willReturn(aDraft(aProposal(Language.NL)));

            // when
            MvcTestResult result = create(PROPOSAL_JSON);

            // then
            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result)
                    .headers()
                    .hasValue("Location", "http://localhost/api/v1/applications/" + APPLICATION_ID.value());
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.reference")
                    .asString()
                    .isEqualTo("CG-2026-000042");
            assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("DRAFT");
            assertThat(result).bodyJson().doesNotHavePath("$.submittedAt");
        }

        static Stream<Arguments> unresolvedReferences() {
            return Stream.of(
                    Arguments.of(FundingCallErrorCode.FUNDING_CALL_NOT_FOUND, "/fundingCallId"),
                    Arguments.of(OrganisationErrorCode.ORGANISATION_NOT_FOUND, "/organisationId"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("unresolvedReferences")
        @DisplayName("answers 400 with a referencedResourceNotFound issue on the reference that doesn't resolve")
        void answers400WithAReferencedResourceNotFoundIssue(ErrorCode code, String field) {
            // given
            given(createApplication.create(COMMAND)).willThrow(new NotFoundException(code, "Not found"));

            // when
            MvcTestResult result = create(PROPOSAL_JSON);

            // then
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
                    .isEqualTo("urn:problem-type:belgif:input-validation:referencedResourceNotFound");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].in")
                    .asString()
                    .isEqualTo("body");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.issues[0].name")
                    .asString()
                    .isEqualTo(field);
        }

        @Test
        @DisplayName("answers 400 requestedAmountExceedsProjectBudget without calling the use case")
        void answers400WhenTheProposalBreaksARuleWithoutCallingTheUseCase() {
            // when
            MvcTestResult result = create(PROPOSAL_JSON.replace("12000.00", "7000.00"));

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:requestedAmountExceedsProjectBudget");
            then(createApplication).shouldHaveNoInteractions();
        }

        private MvcTestResult create(String proposal) {
            return mvc.post()
                    .uri(APPLICATIONS)
                    .with(accessToken(Role.APPLICANT))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"fundingCallId": "%s", "organisationId": "%s", "proposal": %s}
                            """.formatted(FUNDING_CALL_ID.value(), ORGANISATION_ID.value(), proposal))
                    .exchange();
        }
    }

    @Nested
    @DisplayName("Getting an application")
    class Getting {

        @Test
        @DisplayName("returns an application with every field")
        void returnsAnApplicationWithEveryField() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.ASSESSOR))))
                    .willReturn(aSubmittedApplication());

            // when
            MvcTestResult result = get(Role.ASSESSOR);

            // then
            assertThat(result).hasStatusOk();
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asString()
                    .isEqualTo(APPLICATION_ID.value().toString());
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.fundingCallId")
                    .asString()
                    .isEqualTo(FUNDING_CALL_ID.value().toString());
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.organisationId")
                    .asString()
                    .isEqualTo(ORGANISATION_ID.value().toString());
            assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("SUBMITTED");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposal.title")
                    .asString()
                    .isEqualTo("Samen tuinieren in de wijk");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposal.language")
                    .asString()
                    .isEqualTo("NL");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposal.requestedAmount")
                    .satisfies(amount ->
                            assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("8000"));
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposal.totalProjectBudget")
                    .satisfies(amount ->
                            assertThat(new BigDecimal(amount.toString())).isEqualByComparingTo("12000"));
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.submittedAt")
                    .asString()
                    .satisfies(at ->
                            assertThat(OffsetDateTime.parse(at).toInstant()).isEqualTo(SUBMITTED_AT));
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.documents")
                    .asArray()
                    .isEmpty();
        }

        @Test
        @DisplayName("answers 404 resourceNotFound for an application the caller may not see")
        void answers404ForAnApplicationTheCallerMayNotSee() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willThrow(new NotFoundException(APPLICATION_NOT_FOUND, "Application not found"));

            // when
            MvcTestResult result = get(Role.APPLICANT);

            // then
            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:resourceNotFound");
        }

        private MvcTestResult get(Role role) {
            return mvc.get()
                    .uri(APPLICATIONS + "/{applicationId}", APPLICATION_ID.value())
                    .with(accessToken(role))
                    .exchange();
        }
    }

    @Nested
    @DisplayName("Editing a draft")
    class Editing {

        private static final ProjectProposal FRENCH_PROPOSAL = aProposal(Language.FR);
        private static final EditApplicationCommand COMMAND =
                new EditApplicationCommand(APPLICATION_ID, FRENCH_PROPOSAL, actor(Role.APPLICANT));

        @Test
        @DisplayName("replaces the proposal and answers 200 with the stored application")
        void replacesTheProposalAndAnswers200WithTheStoredApplication() {
            // given
            given(getApplication.get(new GetApplicationQuery(APPLICATION_ID, actor(Role.APPLICANT))))
                    .willReturn(aDraft(FRENCH_PROPOSAL));

            // when
            MvcTestResult result = edit();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.proposal.language")
                    .asString()
                    .isEqualTo("FR");
            then(editApplication).should().edit(COMMAND);
        }

        @Test
        @DisplayName("answers 409 applicationNotDraft for an application that was submitted")
        void answers409ForAnApplicationThatWasSubmitted() {
            // given
            willThrow(new ConflictException(APPLICATION_NOT_DRAFT, "Application is SUBMITTED"))
                    .given(editApplication)
                    .edit(COMMAND);

            // when
            MvcTestResult result = edit();

            // then
            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:civicgrant:applicationNotDraft");
        }

        private MvcTestResult edit() {
            return mvc.put()
                    .uri(APPLICATIONS + "/{applicationId}/proposal", APPLICATION_ID.value())
                    .with(accessToken(Role.APPLICANT))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PROPOSAL_JSON.replace("\"NL\"", "\"FR\""))
                    .exchange();
        }
    }

    @Nested
    @DisplayName("Listing applications")
    class Listing {

        @Test
        @DisplayName("returns a middle page with its filters as sets and absolute self, next and prev links")
        void returnsAMiddlePageWithItsFiltersAsSetsAndAbsoluteLinks() {
            // given
            given(searchApplications.search(new SearchApplicationsQuery(
                            Set.of(ApplicationStatus.SUBMITTED), Set.of(FUNDING_CALL_ID), 2, 2, actor(Role.ASSESSOR))))
                    .willReturn(new ApplicationPage(List.of(aSummary()), 2, 2, 5));
            String filtered =
                    "http://localhost/api/v1/applications?status=SUBMITTED&fundingCallId=" + FUNDING_CALL_ID.value();

            // when
            MvcTestResult result = mvc.get()
                    .uri(APPLICATIONS)
                    .param("status", "SUBMITTED")
                    .param("fundingCallId", FUNDING_CALL_ID.value().toString())
                    .param("page", "2")
                    .param("pageSize", "2")
                    .with(accessToken(Role.ASSESSOR))
                    .exchange();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.items[0].id")
                    .asString()
                    .isEqualTo(APPLICATION_ID.value().toString());
            assertThat(result).bodyJson().extractingPath("$.total").asNumber().isEqualTo(5);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.self")
                    .asString()
                    .isEqualTo(filtered + "&page=2&pageSize=2");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.next")
                    .asString()
                    .isEqualTo(filtered + "&page=3&pageSize=2");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.prev")
                    .asString()
                    .isEqualTo(filtered + "&page=1&pageSize=2");
        }

        @Test
        @DisplayName("uses page 1 of 20 by default and leaves out prev and next on a single page")
        void usesPageOneOfTwentyByDefaultAndLeavesOutPrevAndNextOnASinglePage() {
            // given
            given(searchApplications.search(
                            new SearchApplicationsQuery(Set.of(), Set.of(), 1, 20, actor(Role.APPLICANT))))
                    .willReturn(new ApplicationPage(List.of(aSummary()), 1, 20, 1));

            // when
            MvcTestResult result = mvc.get()
                    .uri(APPLICATIONS)
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.self")
                    .asString()
                    .isEqualTo("http://localhost/api/v1/applications?page=1&pageSize=20");
            assertThat(result).bodyJson().doesNotHavePath("$.prev");
            assertThat(result).bodyJson().doesNotHavePath("$.next");
        }

        @Test
        @DisplayName("leaves out next on a last page that is exactly full")
        void leavesOutNextOnALastPageThatIsExactlyFull() {
            // given
            given(searchApplications.search(
                            new SearchApplicationsQuery(Set.of(), Set.of(), 2, 2, actor(Role.APPLICANT))))
                    .willReturn(new ApplicationPage(List.of(aSummary(), aSummary()), 2, 2, 4));

            // when
            MvcTestResult result = mvc.get()
                    .uri(APPLICATIONS)
                    .param("page", "2")
                    .param("pageSize", "2")
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().hasPath("$.prev");
            assertThat(result).bodyJson().doesNotHavePath("$.next");
        }

        @Test
        @DisplayName("answers 400 for a page size above 100 without calling the use case")
        void answers400ForAPageSizeAboveHundredWithoutCallingTheUseCase() {
            // when
            MvcTestResult result = mvc.get()
                    .uri(APPLICATIONS)
                    .param("pageSize", "101")
                    .with(accessToken(Role.APPLICANT))
                    .exchange();

            // then
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.type")
                    .asString()
                    .isEqualTo("urn:problem-type:belgif:badRequest");
            then(searchApplications).shouldHaveNoInteractions();
        }
    }
}
