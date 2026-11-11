package be.achzak.civicgrant.domain.shared;

import static be.achzak.civicgrant.domain.shared.DomainExceptionAssert.assertThatDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Actor")
class ActorTest {

    @Nested
    @DisplayName("Having a role")
    class HasRole {

        @Test
        @DisplayName("has only the roles it was given")
        void hasOnlyTheRolesItWasGiven() {
            // given
            Actor assessor = Actors.anAssessor();

            // when
            boolean isAssessor = assessor.hasRole(Role.ASSESSOR);
            boolean isDecisionMaker = assessor.hasRole(Role.DECISION_MAKER);

            // then
            assertThat(isAssessor).isTrue();
            assertThat(isDecisionMaker).isFalse();
        }
    }

    @Nested
    @DisplayName("Requiring a role")
    class RequireAnyRole {

        @Test
        @DisplayName("accepts an actor holding one of the accepted roles")
        void acceptsActorHoldingOneOfTheAcceptedRoles() {
            // given
            Actor assessor = Actors.anAssessor();

            // when
            Throwable thrown = catchThrowable(() -> assessor.requireAnyRole(Role.APPLICANT, Role.ASSESSOR));

            // then
            assertThat(thrown).isNull();
        }

        @Test
        @DisplayName("refuses an actor holding none of the accepted roles")
        void refusesActorHoldingNoneOfTheAcceptedRoles() {
            // given
            Actor assessor = Actors.anAssessor();

            // when
            Throwable thrown = catchThrowable(() -> assessor.requireAnyRole(Role.APPLICANT, Role.DECISION_MAKER));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        }

        @Test
        @DisplayName("refuses an actor without any role")
        void refusesActorWithoutAnyRole() {
            // given
            Actor actor = Actors.anActorWithoutRoles();

            // when
            Throwable thrown = catchThrowable(() -> actor.requireAnyRole(Role.APPLICANT));

            // then
            assertThatDomainException(thrown).isForbidden().hasCode("ROLE_REQUIRED");
        }
    }
}
