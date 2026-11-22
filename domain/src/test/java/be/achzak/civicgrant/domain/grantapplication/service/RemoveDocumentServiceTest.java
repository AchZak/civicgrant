package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aDraftApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSupportingDocument;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static be.achzak.civicgrant.domain.shared.Actors.anAssessor;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.DocumentId;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.SupportingDocument;
import be.achzak.civicgrant.domain.grantapplication.model.event.DocumentRemoved;
import be.achzak.civicgrant.domain.grantapplication.port.in.RemoveDocumentCommand;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.event.RecordingDomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Removing a supporting document")
class RemoveDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SupportingDocument budget = aSupportingDocument();
    private final SupportingDocument quote = aSupportingDocument();
    private final GrantApplication draft =
            aDraftApplication().withDocuments(budget, quote).build();
    private final GrantApplication submitted =
            aSubmittedApplication().withDocuments(budget).build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(draft, submitted);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final RemoveDocumentService service = new RemoveDocumentService(applications, events, CLOCK);

    private static RemoveDocumentCommand removing(ApplicationId application, DocumentId document, Actor actor) {
        return new RemoveDocumentCommand(application, document, actor);
    }

    @Test
    @DisplayName("removes only that document from the draft")
    void removesOnlyThatDocumentFromTheDraft() {
        // when
        service.remove(removing(draft.id(), budget.id(), anApplicant()));

        // then
        assertThat(applications.findById(draft.id()).orElseThrow().documents()).containsExactly(quote);
    }

    @Test
    @DisplayName("publishes the removal, with the actor and the moment")
    void publishesTheRemoval() {
        // when
        service.remove(removing(draft.id(), budget.id(), anApplicant()));

        // then
        assertThat(events.published())
                .containsExactly(new DocumentRemoved(
                        draft.id(), budget.id(), anApplicant().id(), NOW));
    }

    @Test
    @DisplayName("reports a document the draft doesn't have as not found")
    void reportsADocumentTheDraftDoesNotHaveAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.remove(removing(draft.id(), DocumentId.generate(), anApplicant())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("DOCUMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the application is no longer a draft")
    void publishesNothingWhenTheApplicationIsNoLongerADraft() {
        // when
        Throwable thrown = catchThrowable(() -> service.remove(removing(submitted.id(), budget.id(), anApplicant())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_DRAFT");
        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("refuses an actor without the applicant role")
    void refusesAnActorWithoutTheApplicantRole() {
        // when
        Throwable thrown = catchThrowable(() -> service.remove(removing(draft.id(), budget.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }
}
