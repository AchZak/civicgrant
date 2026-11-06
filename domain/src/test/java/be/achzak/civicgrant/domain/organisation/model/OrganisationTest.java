package be.achzak.civicgrant.domain.organisation.model;

import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import be.achzak.civicgrant.domain.shared.BusinessRuleViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Organisation")
class OrganisationTest {

    private static final Instant NOW = Instant.parse("2026-04-01T09:00:00Z");
    private static final EnterpriseNumber ENTERPRISE_NUMBER = new EnterpriseNumber("0123456749");

    private static Organisation register(String name) {
        return Organisation.create(ENTERPRISE_NUMBER, name, LegalForm.VZW_ASBL, anApplicant(), NOW);
    }

    @Nested
    @DisplayName("Registering")
    class Create {

        @Test
        @DisplayName("records OrganisationRegistered")
        void recordsOrganisationRegistered() {
            // when
            Organisation organisation = register("Buurtwerk vzw");

            // then
            assertThat(organisation.pullEvents())
                    .containsExactly(new OrganisationRegistered(
                            organisation.id(), ENTERPRISE_NUMBER, anApplicant().id(), NOW));
        }

        @Test
        @DisplayName("strips whitespace around the name")
        void stripsWhitespaceAroundName() {
            // when
            Organisation organisation = register("  Buurtwerk vzw ");

            // then
            assertThat(organisation.name()).isEqualTo("Buurtwerk vzw");
        }

        @Test
        @DisplayName("rejects a blank name")
        void rejectsBlankName() {
            // when
            Throwable thrown = catchThrowable(() -> register("   "));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ORGANISATION_NAME_REQUIRED");
        }

        @Test
        @DisplayName("accepts a name of exactly 200 characters")
        void acceptsNameOfExactly200Characters() {
            // given
            String longestName = "a".repeat(200);

            // when
            Organisation organisation = register(longestName);

            // then
            assertThat(organisation.name()).isEqualTo(longestName);
        }

        @Test
        @DisplayName("rejects a name longer than 200 characters")
        void rejectsNameLongerThan200Characters() {
            // given
            String tooLongName = "a".repeat(201);

            // when
            Throwable thrown = catchThrowable(() -> register(tooLongName));

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("code", "ORGANISATION_NAME_TOO_LONG");
        }
    }

    @Nested
    @DisplayName("Restoring")
    class Restore {

        @Test
        @DisplayName("records no events")
        void restoredOrganisationHasNoEvents() {
            // given
            Organisation organisation = Organisation.restore(
                    OrganisationId.generate(), ENTERPRISE_NUMBER, "Buurtwerk vzw", LegalForm.VZW_ASBL, NOW);

            // when
            List<OrganisationEvent> events = organisation.pullEvents();

            // then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pulling events")
    class PullEvents {

        @Test
        @DisplayName("hands out each event only once")
        void handsOutEachEventOnce() {
            // given
            Organisation organisation = register("Buurtwerk vzw");
            organisation.pullEvents();

            // when
            List<OrganisationEvent> secondPull = organisation.pullEvents();

            // then
            assertThat(secondPull).isEmpty();
        }
    }
}
