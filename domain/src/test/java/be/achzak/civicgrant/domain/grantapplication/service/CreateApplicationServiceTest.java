package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.fundingcall.model.FundingCallBuilder.aFundingCall;
import static be.achzak.civicgrant.domain.grantapplication.model.ProjectProposalBuilder.aProjectProposal;
import static be.achzak.civicgrant.domain.organisation.model.OrganisationBuilder.anOrganisation;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.fundingcall.model.FundingCall;
import be.achzak.civicgrant.domain.fundingcall.model.FundingCallId;
import be.achzak.civicgrant.domain.fundingcall.port.out.InMemoryFundingCallRepository;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationCreated;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationReference;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.ProjectProposal;
import be.achzak.civicgrant.domain.grantapplication.port.in.CreateApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryApplicationReferenceSequence;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.organisation.model.LegalForm;
import be.achzak.civicgrant.domain.organisation.model.Organisation;
import be.achzak.civicgrant.domain.organisation.model.OrganisationId;
import be.achzak.civicgrant.domain.organisation.port.out.InMemoryOrganisationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.RecordingDomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Creating a grant application")
class CreateApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final FundingCall FUNDING_CALL = aFundingCall().build();
    private static final Organisation ORGANISATION = anOrganisation().build();
    private static final Organisation COMPANY =
            anOrganisation().withLegalForm(LegalForm.COMPANY).build();
    private static final ProjectProposal PROPOSAL = aProjectProposal().build();

    private final InMemoryGrantApplicationRepository applications = new InMemoryGrantApplicationRepository();
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final CreateApplicationService service = new CreateApplicationService(
            applications,
            new InMemoryFundingCallRepository(FUNDING_CALL),
            new InMemoryOrganisationRepository(ORGANISATION, COMPANY),
            new InMemoryApplicationReferenceSequence(),
            events,
            CLOCK);

    private static CreateApplicationCommand creation(
            FundingCallId fundingCall, OrganisationId organisation, Actor actor) {
        return new CreateApplicationCommand(fundingCall, organisation, PROPOSAL, actor);
    }

    @Test
    @DisplayName("saves a draft numbered with the current year and returns its id")
    void savesADraftNumberedWithTheCurrentYear() {
        // when
        ApplicationId id = service.create(creation(FUNDING_CALL.id(), ORGANISATION.id(), anApplicant()));

        // then
        assertThat(applications.findById(id))
                .get()
                .extracting(
                        GrantApplication::reference,
                        GrantApplication::status,
                        GrantApplication::applicant,
                        GrantApplication::proposal)
                .containsExactly(
                        new ApplicationReference("CG-2026-000001"),
                        ApplicationStatus.DRAFT,
                        anApplicant().id(),
                        PROPOSAL);
    }

    @Test
    @DisplayName("publishes the creation, with the call, the organisation, the actor and the moment")
    void publishesTheCreation() {
        // when
        ApplicationId id = service.create(creation(FUNDING_CALL.id(), ORGANISATION.id(), anApplicant()));

        // then
        assertThat(events.published())
                .containsExactly(new ApplicationCreated(
                        id, FUNDING_CALL.id(), ORGANISATION.id(), anApplicant().id(), NOW));
    }

    @Test
    @DisplayName("refuses an unknown funding call")
    void refusesAnUnknownFundingCall() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.create(creation(FundingCallId.generate(), ORGANISATION.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("FUNDING_CALL_NOT_FOUND");
    }

    @Test
    @DisplayName("refuses an unknown organisation")
    void refusesAnUnknownOrganisation() {
        // when
        Throwable thrown = catchThrowable(
                () -> service.create(creation(FUNDING_CALL.id(), OrganisationId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("ORGANISATION_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the domain refuses the application")
    void publishesNothingWhenTheDomainRefuses() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.create(creation(FUNDING_CALL.id(), COMPANY.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isBusinessRuleViolation().hasCode("LEGAL_FORM_NOT_ELIGIBLE");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("refuses an actor without the applicant role")
    void refusesAnActorWithoutTheApplicantRole() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.create(creation(FUNDING_CALL.id(), ORGANISATION.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
