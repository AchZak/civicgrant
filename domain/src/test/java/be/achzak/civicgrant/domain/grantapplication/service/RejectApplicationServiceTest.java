package be.achzak.civicgrant.domain.grantapplication.service;

import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.aSubmittedApplication;
import static be.achzak.civicgrant.domain.grantapplication.model.GrantApplicationBuilder.anAssessedApplication;
import static be.achzak.civicgrant.domain.shared.Actors.aDecisionMaker;
import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.error.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.grantapplication.model.ApplicationId;
import be.achzak.civicgrant.domain.grantapplication.model.ApplicationStatus;
import be.achzak.civicgrant.domain.grantapplication.model.Decision;
import be.achzak.civicgrant.domain.grantapplication.model.GrantApplication;
import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import be.achzak.civicgrant.domain.grantapplication.model.event.ApplicationRejected;
import be.achzak.civicgrant.domain.grantapplication.port.in.RejectApplicationCommand;
import be.achzak.civicgrant.domain.grantapplication.port.out.InMemoryGrantApplicationRepository;
import be.achzak.civicgrant.domain.shared.Actor;
import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.event.RecordingDomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Rejecting a grant application")
class RejectApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T09:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String MOTIVATION = "Het projectplan is onvoldoende uitgewerkt.";

    private final GrantApplication assessed = anAssessedApplication().build();
    private final GrantApplication submitted = aSubmittedApplication().build();
    private final InMemoryGrantApplicationRepository applications =
            new InMemoryGrantApplicationRepository(assessed, submitted);
    private final RecordingDomainEventPublisher events = new RecordingDomainEventPublisher();
    private final RejectApplicationService service = new RejectApplicationService(applications, events, CLOCK);

    @Test
    @DisplayName("rejects on the given ground by the decision maker at the current moment")
    void rejectsOnTheGivenGroundByTheDecisionMakerAtTheCurrentMoment() {
        // when
        service.reject(rejectionOf(assessed.id(), aDecisionMaker()));

        // then
        GrantApplication stored = applications.findById(assessed.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(stored.decision())
                .contains(new Decision.Rejection(
                        RejectionGround.INSUFFICIENT_QUALITY, aDecisionMaker().id(), MOTIVATION, NOW));
    }

    @Test
    @DisplayName("publishes the rejection, with the ground, the actor and the moment")
    void publishesTheRejection() {
        // when
        service.reject(rejectionOf(assessed.id(), aDecisionMaker()));

        // then
        assertThat(events.published())
                .containsExactly(new ApplicationRejected(
                        assessed.id(),
                        RejectionGround.INSUFFICIENT_QUALITY,
                        aDecisionMaker().id(),
                        NOW));
    }

    @Test
    @DisplayName("reports an unknown application as not found")
    void reportsAnUnknownApplicationAsNotFound() {
        // when
        Throwable thrown =
                catchThrowable(() -> service.reject(rejectionOf(ApplicationId.generate(), aDecisionMaker())));

        // then
        assertThatDomainException(thrown).isNotFound().hasCode("APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("publishes nothing when the domain refuses the rejection")
    void publishesNothingWhenTheDomainRefuses() {
        // when
        Throwable thrown = catchThrowable(() -> service.reject(rejectionOf(submitted.id(), aDecisionMaker())));

        // then
        assertThatDomainException(thrown).isConflict().hasCode("APPLICATION_NOT_ASSESSED");
        assertThat(events.published()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "DECISION_MAKER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("refuses an actor without the decision maker role")
    void refusesAnActorWithoutTheDecisionMakerRole(Role role) {
        // when
        Throwable thrown = catchThrowable(() -> service.reject(rejectionOf(assessed.id(), actor("staff-1", role))));

        // then
        assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
    }

    private RejectApplicationCommand rejectionOf(ApplicationId applicationId, Actor actor) {
        return new RejectApplicationCommand(applicationId, RejectionGround.INSUFFICIENT_QUALITY, MOTIVATION, actor);
    }
}
