package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aRejectedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anApprovedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.shared.ConflictException;
import be.achzak.civicgrant.domain.shared.NotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Grant application lifecycle")
class GrantApplicationLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final FundingCall CALL = aFundingCall().build();
    private static final SupportingDocument DOCUMENT = aSupportingDocument();

    enum Action {
        EDIT(
                ApplicationStatus.DRAFT,
                ApplicationStatus.DRAFT,
                application -> application.edit(aProjectProposal().build(), CALL, anApplicant(), NOW)),
        ATTACH_DOCUMENT(
                ApplicationStatus.DRAFT,
                ApplicationStatus.DRAFT,
                application -> application.attachDocument(
                        "offerte.pdf", SupportingDocument.CONTENT_TYPE, 1024, CALL, anApplicant(), NOW)),
        REMOVE_DOCUMENT(
                ApplicationStatus.DRAFT,
                ApplicationStatus.DRAFT,
                application -> application.removeDocument(DOCUMENT.id(), anApplicant(), NOW)),
        SUBMIT(
                ApplicationStatus.DRAFT,
                ApplicationStatus.SUBMITTED,
                application -> application.submit(CALL, anApplicant(), NOW)),
        RECORD_ASSESSMENT(
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.ASSESSED,
                application -> application.recordAssessment(
                        true, new Recommendation.Approve(euros("8000.00")), "Sterk buurtproject.", anAssessor(), NOW)),
        APPROVE(
                ApplicationStatus.ASSESSED,
                ApplicationStatus.APPROVED,
                application -> application.approve(euros("8000.00"), "Goed onderbouwd.", aDecisionMaker(), NOW)),
        REJECT(
                ApplicationStatus.ASSESSED,
                ApplicationStatus.REJECTED,
                application ->
                        application.reject(RejectionGround.INCOMPLETE, "Onvolledig dossier.", aDecisionMaker(), NOW));

        private final ApplicationStatus from;
        private final ApplicationStatus to;
        private final Consumer<GrantApplication> perform;

        Action(ApplicationStatus from, ApplicationStatus to, Consumer<GrantApplication> perform) {
            this.from = from;
            this.to = to;
            this.perform = perform;
        }
    }

    private static GrantApplication inStatus(ApplicationStatus status) {
        GrantApplicationBuilder builder = switch (status) {
            case DRAFT -> aDraftApplication();
            case SUBMITTED -> aSubmittedApplication();
            case ASSESSED -> anAssessedApplication();
            case APPROVED -> anApprovedApplication();
            case REJECTED -> aRejectedApplication();
        };
        return builder.forCall(CALL).withDocuments(DOCUMENT).build();
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Arrays.stream(Action.values())
                .flatMap(action -> Arrays.stream(ApplicationStatus.values())
                        .filter(status -> status != action.from)
                        .map(status -> Arguments.of(action, status)));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Action.class)
    @DisplayName("allows each action in its own status, moving to the next one")
    void allowsEachActionInItsOwnStatus(Action action) {
        // given
        GrantApplication application = inStatus(action.from);

        // when
        action.perform.accept(application);

        // then
        assertThat(application.status()).isEqualTo(action.to);
        assertThat(application.pullEvents()).hasSize(1);
    }

    @ParameterizedTest(name = "{0} on a {1} application")
    @MethodSource("forbiddenTransitions")
    @DisplayName("refuses each action in any other status, changing nothing")
    void refusesEachActionInAnyOtherStatus(Action action, ApplicationStatus status) {
        // given
        GrantApplication application = inStatus(status);

        // when
        Throwable thrown = catchThrowable(() -> action.perform.accept(application));

        // then
        assertThat(thrown).isInstanceOfAny(ConflictException.class, NotFoundException.class);
        assertThat(application.status()).isEqualTo(status);
        assertThat(application.pullEvents()).isEmpty();
    }
}
