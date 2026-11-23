package be.achzak.civicgrant.core;

import static be.achzak.civicgrant.core.common.domain.Money.euros;
import static be.achzak.civicgrant.core.common.domain.error.DomainExceptionAssert.assertThatDomainException;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.actor;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.anApplicant;
import static be.achzak.civicgrant.core.common.domain.identity.Actors.anotherApplicant;
import static be.achzak.civicgrant.core.common.domain.identity.Role.APPLICANT;
import static be.achzak.civicgrant.core.common.domain.identity.Role.ASSESSOR;
import static be.achzak.civicgrant.core.common.domain.identity.Role.DECISION_MAKER;
import static be.achzak.civicgrant.core.common.domain.identity.Role.PROGRAMME_MANAGER;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.PdfFiles.aPdf;
import static be.achzak.civicgrant.core.grantapplication.domain.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.core.organisation.domain.OrganisationBuilder.anOrganisation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import be.achzak.civicgrant.core.common.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.common.domain.LocalizedText;
import be.achzak.civicgrant.core.common.domain.error.ForbiddenException;
import be.achzak.civicgrant.core.common.domain.identity.Actor;
import be.achzak.civicgrant.core.common.domain.identity.Role;
import be.achzak.civicgrant.core.fundingcall.application.port.in.GetFundingCallQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.in.ListOpenFundingCallsQuery;
import be.achzak.civicgrant.core.fundingcall.application.port.in.OpenFundingCallCommand;
import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.application.service.GetFundingCallService;
import be.achzak.civicgrant.core.fundingcall.application.service.ListOpenFundingCallsService;
import be.achzak.civicgrant.core.fundingcall.application.service.OpenFundingCallService;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallId;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCallReference;
import be.achzak.civicgrant.core.fundingcall.domain.SubmissionPeriod;
import be.achzak.civicgrant.core.grantapplication.application.port.in.ApproveApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.AttachDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.DownloadDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.EditApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetApplicationQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetAssessmentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.GetDocumentQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SearchApplicationsQuery;
import be.achzak.civicgrant.core.grantapplication.application.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryApplicationReferenceSequence;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryDocumentStorage;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationPage;
import be.achzak.civicgrant.core.grantapplication.application.search.ApplicationSummary;
import be.achzak.civicgrant.core.grantapplication.application.service.ApproveApplicationService;
import be.achzak.civicgrant.core.grantapplication.application.service.AttachDocumentService;
import be.achzak.civicgrant.core.grantapplication.application.service.CreateApplicationService;
import be.achzak.civicgrant.core.grantapplication.application.service.DownloadDocumentService;
import be.achzak.civicgrant.core.grantapplication.application.service.EditApplicationService;
import be.achzak.civicgrant.core.grantapplication.application.service.GetApplicationService;
import be.achzak.civicgrant.core.grantapplication.application.service.GetAssessmentService;
import be.achzak.civicgrant.core.grantapplication.application.service.GetDocumentService;
import be.achzak.civicgrant.core.grantapplication.application.service.RecordAssessmentService;
import be.achzak.civicgrant.core.grantapplication.application.service.RejectApplicationService;
import be.achzak.civicgrant.core.grantapplication.application.service.RemoveDocumentService;
import be.achzak.civicgrant.core.grantapplication.application.service.SearchApplicationsService;
import be.achzak.civicgrant.core.grantapplication.application.service.SubmitApplicationService;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.DocumentId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.grantapplication.domain.RejectionGround;
import be.achzak.civicgrant.core.organisation.application.port.in.GetOrganisationQuery;
import be.achzak.civicgrant.core.organisation.application.port.in.RegisterOrganisationCommand;
import be.achzak.civicgrant.core.organisation.application.port.in.SearchOrganisationsQuery;
import be.achzak.civicgrant.core.organisation.application.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.core.organisation.application.service.GetOrganisationService;
import be.achzak.civicgrant.core.organisation.application.service.RegisterOrganisationService;
import be.achzak.civicgrant.core.organisation.application.service.SearchOrganisationsService;
import be.achzak.civicgrant.core.organisation.domain.EnterpriseNumber;
import be.achzak.civicgrant.core.organisation.domain.LegalForm;
import be.achzak.civicgrant.core.organisation.domain.OrganisationId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Access to the use cases")
class UseCaseAccessTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ApplicationId APPLICATION = ApplicationId.generate();
    private static final DocumentId DOCUMENT = DocumentId.generate();
    private static final ProjectProposal PROPOSAL = aProjectProposal().build();
    private static final EnterpriseNumber ENTERPRISE_NUMBER =
            anOrganisation().build().enterpriseNumber();
    private static final String MOTIVATION = "Het project versterkt de buurt.";

    private static final List<UseCase> USE_CASES = List.of(
            useCase(
                    "open a funding call",
                    EnumSet.of(PROGRAMME_MANAGER),
                    actor -> new OpenFundingCallService(fundingCalls(), events(), CLOCK)
                            .open(new OpenFundingCallCommand(
                                    new FundingCallReference("GRN-2027"),
                                    new LocalizedText("Groene daken 2027", "Toitures vertes 2027"),
                                    new SubmissionPeriod(NOW.plus(Duration.ofDays(14)), NOW.plus(Duration.ofDays(120))),
                                    euros("80000.00"),
                                    euros("20000.00"),
                                    actor))),
            useCase(
                    "get a funding call",
                    EnumSet.allOf(Role.class),
                    actor -> new GetFundingCallService(fundingCalls())
                            .get(new GetFundingCallQuery(FundingCallId.generate(), actor))),
            useCase(
                    "list the open funding calls",
                    EnumSet.allOf(Role.class),
                    actor -> new ListOpenFundingCallsService(fundingCalls(), CLOCK)
                            .list(new ListOpenFundingCallsQuery(actor))),
            useCase(
                    "register an organisation",
                    EnumSet.of(APPLICANT),
                    actor -> new RegisterOrganisationService(organisations(), events(), CLOCK)
                            .register(new RegisterOrganisationCommand(
                                    ENTERPRISE_NUMBER, "Buurtwerk Noord", LegalForm.VZW_ASBL, actor))),
            useCase(
                    "get an organisation",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor -> new GetOrganisationService(organisations())
                            .get(new GetOrganisationQuery(OrganisationId.generate(), actor))),
            useCase(
                    "search organisations",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor -> new SearchOrganisationsService(organisations())
                            .search(new SearchOrganisationsQuery(ENTERPRISE_NUMBER, actor))),
            useCase(
                    "create an application",
                    EnumSet.of(APPLICANT),
                    actor -> new CreateApplicationService(
                                    applications(),
                                    fundingCalls(),
                                    organisations(),
                                    new InMemoryApplicationReferenceSequence(),
                                    events(),
                                    CLOCK)
                            .create(new CreateApplicationCommand(
                                    FundingCallId.generate(), OrganisationId.generate(), PROPOSAL, actor))),
            useCase(
                    "edit an application",
                    EnumSet.of(APPLICANT),
                    actor -> new EditApplicationService(applications(), fundingCalls(), events(), CLOCK)
                            .edit(new EditApplicationCommand(APPLICATION, PROPOSAL, actor))),
            useCase(
                    "attach a document",
                    EnumSet.of(APPLICANT),
                    actor -> new AttachDocumentService(
                                    applications(), fundingCalls(), new InMemoryDocumentStorage(), events(), CLOCK)
                            .attach(new AttachDocumentCommand(
                                    APPLICATION, "begroting.pdf", "application/pdf", aPdf(), actor))),
            useCase(
                    "remove a document",
                    EnumSet.of(APPLICANT),
                    actor -> new RemoveDocumentService(applications(), events(), CLOCK)
                            .remove(new RemoveDocumentCommand(APPLICATION, DOCUMENT, actor))),
            useCase(
                    "submit an application",
                    EnumSet.of(APPLICANT),
                    actor -> new SubmitApplicationService(applications(), fundingCalls(), events(), CLOCK)
                            .submit(new SubmitApplicationCommand(APPLICATION, actor))),
            useCase(
                    "get an application",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor ->
                            new GetApplicationService(applications()).get(new GetApplicationQuery(APPLICATION, actor))),
            useCase(
                    "search applications",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor -> new SearchApplicationsService(applications())
                            .search(new SearchApplicationsQuery(Set.of(), Set.of(), 1, 20, actor))),
            useCase(
                    "get a document",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor -> new GetDocumentService(applications())
                            .get(new GetDocumentQuery(APPLICATION, DOCUMENT, actor))),
            useCase(
                    "download a document",
                    EnumSet.of(APPLICANT, ASSESSOR, DECISION_MAKER),
                    actor -> new DownloadDocumentService(applications(), new InMemoryDocumentStorage())
                            .download(new DownloadDocumentQuery(APPLICATION, DOCUMENT, actor))),
            useCase(
                    "record an assessment",
                    EnumSet.of(ASSESSOR),
                    actor -> new RecordAssessmentService(applications(), events(), CLOCK)
                            .record(new RecordAssessmentCommand(
                                    APPLICATION, true, new Recommendation.Reject(), "Onvoldoende onderbouwd.", actor))),
            useCase(
                    "get an assessment",
                    EnumSet.of(ASSESSOR, DECISION_MAKER),
                    actor -> new GetAssessmentService(applications()).get(new GetAssessmentQuery(APPLICATION, actor))),
            useCase(
                    "approve an application",
                    EnumSet.of(DECISION_MAKER),
                    actor -> new ApproveApplicationService(applications(), fundingCalls(), events(), CLOCK)
                            .approve(new ApproveApplicationCommand(APPLICATION, euros("7500.00"), MOTIVATION, actor))),
            useCase(
                    "reject an application",
                    EnumSet.of(DECISION_MAKER),
                    actor -> new RejectApplicationService(applications(), events(), CLOCK)
                            .reject(new RejectApplicationCommand(
                                    APPLICATION, RejectionGround.INSUFFICIENT_QUALITY, MOTIVATION, actor))));

    private static final Condition<Throwable> REFUSED_FOR_ITS_ROLE = new Condition<>(
            thrown -> thrown instanceof ForbiddenException refusal
                    && refusal.code().name().equals("ROLE_REQUIRED"),
            "a refusal for a missing role");

    @Nested
    @DisplayName("Checking the caller's role")
    class Roles {

        static Stream<Arguments> listedRoles() {
            return USE_CASES.stream()
                    .flatMap(useCase -> useCase.roles().stream().map(role -> arguments(useCase, role)));
        }

        static Stream<Arguments> otherRoles() {
            return USE_CASES.stream()
                    .flatMap(useCase ->
                            EnumSet.complementOf(useCase.roles()).stream().map(role -> arguments(useCase, role)));
        }

        @ParameterizedTest(name = "{0} as {1}")
        @MethodSource("listedRoles")
        @DisplayName("lets every role the use case lists through")
        void letsEveryListedRoleThrough(UseCase useCase, Role role) {
            // when
            Throwable thrown = catchThrowable(() -> useCase.call().accept(actor("user-1", role)));

            // then
            assertThat(thrown).isNot(REFUSED_FOR_ITS_ROLE);
        }

        @ParameterizedTest(name = "{0} as {1}")
        @MethodSource("otherRoles")
        @DisplayName("refuses every other role")
        void refusesEveryOtherRole(UseCase useCase, Role role) {
            // when
            Throwable thrown = catchThrowable(() -> useCase.call().accept(actor("user-1", role)));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        }
    }

    @Nested
    @DisplayName("Hiding applications from other applicants")
    class Visibility {

        @Test
        @DisplayName("hides another applicant's application, as if it didn't exist")
        void hidesAnotherApplicantsApplication() {
            // given
            GrantApplication draft = aDraftApplication().build();
            GetApplicationService service = new GetApplicationService(new InMemoryGrantApplicationRepository(draft));

            // when
            Throwable thrown =
                    catchThrowable(() -> service.get(new GetApplicationQuery(draft.id(), anotherApplicant())));

            // then
            assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
        }

        @Test
        @DisplayName("finds only the applicant's own applications in a search")
        void findsOnlyTheApplicantsOwnApplicationsInASearch() {
            // given
            GrantApplication mine = aSubmittedApplication().build();
            GrantApplication someoneElses = aSubmittedApplication()
                    .withApplicant(anotherApplicant().id())
                    .build();
            SearchApplicationsService service =
                    new SearchApplicationsService(new InMemoryGrantApplicationRepository(mine, someoneElses));

            // when
            ApplicationPage page =
                    service.search(new SearchApplicationsQuery(Set.of(), Set.of(), 1, 20, anApplicant()));

            // then
            assertThat(page.items()).extracting(ApplicationSummary::id).containsExactly(mine.id());
        }
    }

    private static UseCase useCase(String name, EnumSet<Role> roles, Consumer<Actor> call) {
        return new UseCase(name, roles, call);
    }

    private static InMemoryFundingCallRepository fundingCalls() {
        return new InMemoryFundingCallRepository();
    }

    private static InMemoryOrganisationRepository organisations() {
        return new InMemoryOrganisationRepository();
    }

    private static InMemoryGrantApplicationRepository applications() {
        return new InMemoryGrantApplicationRepository();
    }

    private static RecordingDomainEventPublisher events() {
        return new RecordingDomainEventPublisher();
    }

    record UseCase(String name, EnumSet<Role> roles, Consumer<Actor> call) {

        @Override
        public String toString() {
            return name;
        }
    }
}
