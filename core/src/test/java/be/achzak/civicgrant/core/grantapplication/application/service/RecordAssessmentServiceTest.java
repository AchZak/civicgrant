package be.achzak.civicgrant.core.grantapplication.application.service;

import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.actor;
import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anAssessor;
import static be.achzak.civicgrant.core.sharedkernel.domain.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.core.grantapplication.application.port.in.RecordAssessmentCommand;
import be.achzak.civicgrant.core.grantapplication.application.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationId;
import be.achzak.civicgrant.core.grantapplication.domain.ApplicationStatus;
import be.achzak.civicgrant.core.grantapplication.domain.Assessment;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplication;
import be.achzak.civicgrant.core.grantapplication.domain.Recommendation;
import be.achzak.civicgrant.core.grantapplication.domain.event.AssessmentRecorded;
import be.achzak.civicgrant.core.sharedkernel.application.port.out.RecordingDomainEventPublisher;
import be.achzak.civicgrant.core.sharedkernel.domain.Actor;
import be.achzak.civicgrant.core.sharedkernel.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Recording an assessment")
class RecordAssessmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-04T14:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String COMMENT = "Sterk buurtproject met een realistische begroting.";

    private final GrantApplication submitted = aSubmittedApplication().build();
    private final GrantApplication assessed = anAssessedApplication().build();
    private final Recommendation approval =
            new Recommendation.Approve(submitted.proposal().requestedAmount());
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(submitted, assessed);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final RecordAssessmentService service = new RecordAssessmentService(applications, events, CLOCK);

    @Test
    @DisplayName("records the assessment by the assessor at the current moment")
    void recordsTheAssessmentByTheAssessorAtTheCurrentMoment() {
        // when
        service.record(assessmentOf(submitted.id(), anAssessor()));

        // then
        GrantApplication stored = applications.findById(submitted.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ApplicationStatus.ASSESSED);
        assertThat(stored.assessment()).contains(new Assessment(anAssessor().id(), true, approval, COMMENT, NOW));
    }

    @Test
    @DisplayName("publishes the assessment, with the eligibility, the recommendation, the actor and the moment")
    void publishesTheAssessment() {
        // given
        Recommendation rejection = new Recommendation.Reject();

        // when
        service.record(new RecordAssessmentCommand(submitted.id(), false, rejection, COMMENT, anAssessor()));

        // then
        assertThat(events.published())
                .containsExactly(new AssessmentRecorded(
                        submitted.id(), false, rejection, anAssessor().id(), NOW));
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown = catchThrowable(() -> service.record(assessmentOf(ApplicationId.generate(), anAssessor())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the domain refuses the assessment")
    void publishesNothingWhenTheDomainRefuses() {
        // when
        Throwable thrown = catchThrowable(() -> service.record(assessmentOf(assessed.id(), anAssessor())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_SUBMITTED");
        assertThat(events.published()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ASSESSOR", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("refuses an actor without the assessor role")
    void refusesAnActorWithoutTheAssessorRole(Role role) {
        // when
        Throwable thrown = catchThrowable(() -> service.record(assessmentOf(submitted.id(), actor("staff-1", role))));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    private RecordAssessmentCommand assessmentOf(ApplicationId applicationId, Actor actor) {
        return new RecordAssessmentCommand(applicationId, true, approval, COMMENT, actor);
    }
}
