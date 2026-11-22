package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.fundingcall.domain.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anAssessor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anotherApplicant;
import static be.achzak.civicgrant.core.sharedkernel.domain.Money.euros;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.fundingcall.application.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.core.fundingcall.domain.FundingCall;
import be.achzak.civicgrant.core.grantapplication.application.port.in.EditApplicationCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.ProjectProposal;
import be.achzak.civicgrant.core.grantapplication.domain.event.ApplicationEdited;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Editing a grant application")
class EditApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final FundingCall FUNDING_CALL = aFundingCall().build();
    private static final ProjectProposal NEW_PROPOSAL = aProjectProposal()
            .withTitle("Moestuin met composthoek")
            .withRequestedAmount(euros("9500.00"))
            .build();

    private final GrantApplication draft =
            aDraftApplication().forCall(FUNDING_CALL).build();
    private final GrantApplication submitted =
            aSubmittedApplication().forCall(FUNDING_CALL).build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(draft, submitted);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final EditApplicationService service =
            new EditApplicationService(applications, new InMemoryFundingCallRepository(FUNDING_CALL), events, CLOCK);

    private static EditApplicationCommand edition(ApplicationId application, Actor actor) {
        return new EditApplicationCommand(application, NEW_PROPOSAL, actor);
    }

    @Test
    @DisplayName("replaces the proposal of the draft")
    void replacesTheProposalOfTheDraft() {
        // when
        service.edit(edition(draft.id(), anApplicant()));

        // then
        assertThat(applications.findById(draft.id()))
                .get()
                .extracting(GrantApplication::proposal)
                .isEqualTo(NEW_PROPOSAL);
    }

    @Test
    @DisplayName("publishes the edit, with the new requested amount, the actor and the moment")
    void publishesTheEdit() {
        // when
        service.edit(edition(draft.id(), anApplicant()));

        // then
        assertThat(events.published())
                .containsExactly(new ApplicationEdited(
                        draft.id(), euros("9500.00"), anApplicant().id(), NOW));
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown = catchThrowable(() -> service.edit(edition(ApplicationId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("hides a draft from another applicant, as if it didn't exist")
    void hidesADraftFromAnotherApplicant() {
        // when
        Throwable thrown = catchThrowable(() -> service.edit(edition(draft.id(), anotherApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the domain refuses the edit")
    void publishesNothingWhenTheDomainRefuses() {
        // when
        Throwable thrown = catchThrowable(() -> service.edit(edition(submitted.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("refuses an actor without the applicant role")
    void refusesAnActorWithoutTheApplicantRole() {
        // when
        Throwable thrown = catchThrowable(() -> service.edit(edition(draft.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
