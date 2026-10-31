package be.achzak.civicgrant.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Actor")
class ActorTest {

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
