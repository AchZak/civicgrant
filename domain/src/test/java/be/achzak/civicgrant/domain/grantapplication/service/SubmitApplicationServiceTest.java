package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.Actors.anotherApplicant;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationSubmitted;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.port.in.SubmitApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.RecordingDomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Submitting a grant application")
class SubmitApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final FundingCall FUNDING_CALL = aFundingCall().build();

    private final GrantApplication draft =
            aDraftApplication().forCall(FUNDING_CALL).build();
    private final GrantApplication draftWithoutDocuments =
            aDraftApplication().forCall(FUNDING_CALL).withoutDocuments().build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(draft, draftWithoutDocuments);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final SubmitApplicationService service =
            new SubmitApplicationService(applications, new InMemoryFundingCallRepository(FUNDING_CALL), events, CLOCK);

    @Test
    @DisplayName("submits the draft at the current moment")
    void submitsTheDraftAtTheCurrentMoment() {
        // when
        service.submit(new SubmitApplicationCommand(draft.id(), anApplicant()));

        // then
        GrantApplication stored = applications.findById(draft.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(stored.submittedAt()).contains(NOW);
    }

    @Test
    @DisplayName("publishes the submission, with the requested amount, the actor and the moment")
    void publishesTheSubmission() {
        // when
        service.submit(new SubmitApplicationCommand(draft.id(), anApplicant()));

        // then
        assertThat(events.published())
                .containsExactly(new ApplicationSubmitted(
                        draft.id(),
                        draft.proposal().requestedAmount(),
                        anApplicant().id(),
                        NOW));
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.submit(new SubmitApplicationCommand(ApplicationId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("hides a draft from another applicant, as if it didn't exist")
    void hidesADraftFromAnotherApplicant() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.submit(new SubmitApplicationCommand(draft.id(), anotherApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the domain refuses the submission")
    void publishesNothingWhenTheDomainRefuses() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.submit(new SubmitApplicationCommand(draftWithoutDocuments.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("SUPPORTING_DOCUMENT_REQUIRED");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("refuses an actor without the applicant role")
    void refusesAnActorWithoutTheApplicantRole() {
        // when
        Throwable thrown = catchThrowable(() -> service.submit(new SubmitApplicationCommand(draft.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
