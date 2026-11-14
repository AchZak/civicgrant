package be.achzak.civicgrant.domain.grantapplication.model;

import static be.achzak.civicgrant.domain.shared.Actors.actor;
import static be.achzak.civicgrant.domain.shared.Actors.anActorWithoutRoles;
import static be.achzak.civicgrant.domain.shared.Actors.anApplicant;
import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.domain.shared.Role;
import be.achzak.civicgrant.domain.shared.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Application visibility")
class ApplicationVisibilityTest {

    private static final UserId ME = new UserId("me");
    private static final UserId SOMEONE_ELSE = new UserId("someone-else");

    @Nested
    @DisplayName("Deriving it from an actor")
    class Of {

        @Test
        @DisplayName("keeps the actor's id")
        void keepsTheActorsId() {
            // when
            ApplicationVisibility visibility = ApplicationVisibility.of(anApplicant());

            // then
            assertThat(visibility.user()).isEqualTo(anApplicant().id());
        }

        @ParameterizedTest
        @EnumSource(
                value = Role.class,
                names = {"ASSESSOR", "DECISION_MAKER"})
        @DisplayName("counts assessors and decision makers as staff")
        void countsAssessorsAndDecisionMakersAsStaff(Role role) {
            // when
            ApplicationVisibility visibility = ApplicationVisibility.of(actor("staff-1", role));

            // then
            assertThat(visibility.staff()).isTrue();
        }

        @Test
        @DisplayName("doesn't count an applicant as staff")
        void doesNotCountAnApplicantAsStaff() {
            // when
            ApplicationVisibility visibility = ApplicationVisibility.of(anApplicant());

            // then
            assertThat(visibility.staff()).isFalse();
        }

        @Test
        @DisplayName("doesn't count an actor without roles as staff")
        void doesNotCountAnActorWithoutRolesAsStaff() {
            // when
            ApplicationVisibility visibility = ApplicationVisibility.of(anActorWithoutRoles());

            // then
            assertThat(visibility.staff()).isFalse();
        }
    }

    @Nested
    @DisplayName("Allowing an application")
    class Allows {

        @ParameterizedTest
        @EnumSource(ApplicationStatus.class)
        @DisplayName("allows applicants their own applications in every status")
        void allowsApplicantsTheirOwnApplicationsInEveryStatus(ApplicationStatus status) {
            // given
            ApplicationVisibility visibility = new ApplicationVisibility(ME, false);

            // when
            boolean allowed = visibility.allows(ME, status);

            // then
            assertThat(allowed).isTrue();
        }

        @ParameterizedTest
        @EnumSource(ApplicationStatus.class)
        @DisplayName("refuses applicants someone else's applications in every status")
        void refusesApplicantsSomeoneElsesApplicationsInEveryStatus(ApplicationStatus status) {
            // given
            ApplicationVisibility visibility = new ApplicationVisibility(ME, false);

            // when
            boolean allowed = visibility.allows(SOMEONE_ELSE, status);

            // then
            assertThat(allowed).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
        @DisplayName("allows staff someone else's application once it left draft")
        void allowsStaffSomeoneElsesApplicationOnceItLeftDraft(ApplicationStatus status) {
            // given
            ApplicationVisibility visibility = new ApplicationVisibility(ME, true);

            // when
            boolean allowed = visibility.allows(SOMEONE_ELSE, status);

            // then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("refuses staff someone else's draft")
        void refusesStaffSomeoneElsesDraft() {
            // given
            ApplicationVisibility visibility = new ApplicationVisibility(ME, true);

            // when
            boolean allowed = visibility.allows(SOMEONE_ELSE, ApplicationStatus.DRAFT);

            // then
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("allows staff their own draft")
        void allowsStaffTheirOwnDraft() {
            // given
            ApplicationVisibility visibility = new ApplicationVisibility(ME, true);

            // when
            boolean allowed = visibility.allows(ME, ApplicationStatus.DRAFT);

            // then
            assertThat(allowed).isTrue();
        }
    }
}
